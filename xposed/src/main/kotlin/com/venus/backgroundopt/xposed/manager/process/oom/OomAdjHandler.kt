/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.xposed.manager.process.oom

import com.venus.backgroundopt.common.entity.message.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.common.entity.message.GlobalOomScorePolicy
import com.venus.backgroundopt.common.entity.preference.getCustomBgAdj
import com.venus.backgroundopt.common.entity.preference.getCustomFgAdj
import com.venus.backgroundopt.common.util.clamp
import com.venus.backgroundopt.common.util.concurrent.ConcurrentUtils
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.ifFalse
import com.venus.backgroundopt.common.util.lock
import com.venus.backgroundopt.common.util.log.logInfo
import com.venus.backgroundopt.xposed.core.AppGroupEnum
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessList
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord.AdjHandleActionType
import com.venus.backgroundopt.xposed.entity.self.AppInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.handle.getCustomMainProcessBgAdj
import com.venus.backgroundopt.xposed.manager.message.handle.getCustomMainProcessFgAdj
import com.venus.backgroundopt.xposed.manager.process.oom.CachedByteBufferThreadFactory.Companion.byteBufferThreadLocal
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * @author XingC
 * @date 2024/7/17
 */
abstract class OomAdjHandler(
    var userProcessMinAdj: Int = 0,
    var userProcessMaxAdj: Int = ProcessList.MAX_ADJ,
    var adjConvertFactor: Int = 1,
    var highPrioritySubprocessAdjOffset: Int = 0,
) : OomAdjHandlerCommonMethods() {
    /* *************************************************************************
     *                                                                         *
     * ADJ应用任务调度                                                           *
     *                                                                         *
     **************************************************************************/
    private val adjTaskPool = ExecutorUtils.newScheduleThreadPool(
        coreSize = ConcurrentUtils.computeIntensiveTaskThreadCount,
        threadFactory = CachedByteBufferThreadFactory(),
        removeOnCancelPolicy = true
    )

    private val adjTaskMap = ConcurrentHashMap<ProcessRecord, ScheduledFuture<*>>()
    private val adjTaskPriorityMap = ConcurrentHashMap<ProcessRecord, Int>()

    private val taskDelay: Long = 3L
    private val taskDelayTimeUnit: TimeUnit = TimeUnit.SECONDS

    /**
     * 以[processRecord]为标识来添加adj的设置任务。
     *
     * 若 未添加任务 || 上一个任务的优先级 <= 当前任务, 则会取消上一次任务(除非它已经开始运行)
     */
    @JvmOverloads
    fun addTask(
        processRecord: ProcessRecord,
        priority: Int = ADJ_TASK_PRIORITY_NORMAL,
        block: () -> Unit,
    ) {
        adjTaskMap.compute(processRecord) { _, lastScheduledFuture ->
            var submitFuture: ScheduledFuture<*>? = null
            adjTaskPriorityMap.compute(processRecord) { _, lastTaskPriority ->
                if (lastTaskPriority == null || priority >= lastTaskPriority) {
                    lastScheduledFuture?.cancel(true)
                    submitFuture = scheduleAdjTask {
                        var isCancelled = false
                        adjTaskMap.lock(processRecord) {
                            isCancelled = submitFuture?.isCancelled == true
                            // 移除记录
                            adjTaskPriorityMap.remove(processRecord)
                            adjTaskMap.remove(processRecord)
                        }
                        isCancelled.ifFalse {
                            block()
                        }
                    }
                    priority
                } else {
                    submitFuture = lastScheduledFuture
                    lastTaskPriority
                }

            }
            submitFuture
        }
    }

    fun scheduleAdjTask(block: () -> Unit): ScheduledFuture<*> {
        return adjTaskPool.schedule(block, taskDelay, taskDelayTimeUnit)
    }

    /* *************************************************************************
     *                                                                         *
     * ADJ计算与应用                                                             *
     *                                                                         *
     **************************************************************************/
    protected val highPriorityProcessNotHasActivityAdjCache = ShortArray(ADJ_CACHE_SIZE) {
        UNINITIALIZED_VALUE
    }
    protected val mainProcessAdjCache = ShortArray(ADJ_CACHE_SIZE) { UNINITIALIZED_VALUE }
    protected val highPrioritySubprocessAdjCache = ShortArray(ADJ_CACHE_SIZE) {
        UNINITIALIZED_VALUE
    }
    protected val subprocessAdjCache = ShortArray(ADJ_CACHE_SIZE) { UNINITIALIZED_VALUE }

    @Volatile
    private var globalOomScorePolicy = HookCommonProperties.globalOomScorePolicy.value
    private val customGlobalOomScore: Int get() = globalOomScorePolicy.customGlobalOomScore

    // 全局oom分数处理器
    @Volatile
    private var globalOomScoreAdjHandler: GlobalOomScoreAdjHandler = getGlobalOomScoreAdjHandler(
        globalOomScorePolicy
    ).also {
        HookCommonProperties.globalOomScorePolicy.addListener(
            GlobalOomScoreAdjHandler.PROPERTY_LISTENER_KEY
        ) { _, newValue ->
            OomAdjustManager.printAdjHandleActionTypeLog("全局OOM切换")
            globalOomScorePolicy = newValue
            globalOomScoreAdjHandler = getGlobalOomScoreAdjHandler(newValue)

            ProcessRecord.resetAdjHandleType()
        }
    }

    private fun getGlobalOomScoreAdjHandler(
        globalOomScorePolicy: GlobalOomScorePolicy,
    ): GlobalOomScoreAdjHandler {
        if (!globalOomScorePolicy.enabled) {
            return DisabledGlobalOomScoreAdjHandler()
        }
        return when (globalOomScorePolicy.globalOomScoreEffectiveScope) {
            GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS -> MainProcessGlobalOomScoreAdjHandler()
            GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS -> MainAndSubProcessGlobalOomScoreAdjHandler()
            GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS_ANY -> MainProcessAnyGlobalOomScoreAdjHandler()
            GlobalOomScoreEffectiveScopeEnum.ALL -> AllGlobalOomScoreAdjHandler()
        }
    }

    public override fun getAdjToCompute(processRecord: ProcessRecord, adj: Int): Int {
        return processRecord.processStateRecord.curComputedAdj
    }

    /**
     * 计算并应用adj
     */
    fun computeAdjAndApply(
        processRecord: ProcessRecord,
        adj: Int,
        priority: Int = ADJ_TASK_PRIORITY_NORMAL,
    ) {
        addTask(processRecord, priority) {
            checkAndApplyAdjUseCachedByteBuffer(
                processRecord = processRecord,
                pid = processRecord.pid,
                uid = processRecord.uid,
                adj = computeAdj(processRecord, adj)
            )
        }
    }

    fun computeAdjAndBatchApply(
        processList: List<ProcessRecord>,
        afterOomSetBlock: (process: ProcessRecord, adjToCompute: Int) -> Unit
    ) {
        if (processList.isEmpty()) return

        val LMK_PROCS_PRIO = ProcessList.LMK_PROCS_PRIO.toInt()
        var buf = ByteBuffer.allocate(ProcessList.MAX_OOM_ADJ_BATCH_LENGTH)
        var total_procs_in_buf = 0
        buf.putInt(LMK_PROCS_PRIO)
        for (processRecord in processList) {
            val pid = processRecord.pid
            val adj = processRecord.processStateRecord.curAdj
            val uid = processRecord.uid
            val adjToCompute = getAdjToCompute(processRecord, adj)

            if (pid <= 0 || adjToCompute == ProcessList.UNKNOWN_ADJ) continue
            if (total_procs_in_buf >= ProcessList.MAX_PROCS_PRIO_PACKET_SIZE) {
                ProcessList.writeLmkd(buf, null)
                buf.clear()
                total_procs_in_buf = 0
                buf = ByteBuffer.allocate(ProcessList.MAX_OOM_ADJ_BATCH_LENGTH)
                buf.putInt(LMK_PROCS_PRIO)
            }
            buf.putInt(pid)
            buf.putInt(uid)
            buf.putInt(computeAdj(processRecord, adjToCompute))
            buf.putInt(0)  // Default proc type to PROC_TYPE_APP
            total_procs_in_buf++
            afterOomSetBlock(processRecord, adjToCompute)
        }
        ProcessList.writeLmkd(buf, null)
    }

    /**
     * 应用给定的adj
     */
    protected fun applyAdj(pid: Int, uid: Int, adj: Int) {
        ProcessList.writeLmkd(pid, uid, adj)
    }

    protected fun applyAdjUseCachedByteBuffer(pid: Int, uid: Int, adj: Int) {
        val byteBuffer = byteBufferThreadLocal.get()!!
        try {
            ProcessList.writeLmkd(byteBuffer, pid, uid, adj)
        } finally {
            byteBuffer.clear()
        }
    }

    protected fun checkAndApplyAdjUseCachedByteBuffer(
        processRecord: ProcessRecord,
        pid: Int,
        uid: Int,
        adj: Int,
    ) {
        applyAdjUseCachedByteBuffer(
            pid = pid,
            uid = uid,
            adj = clamp(adj, min = ProcessList.NATIVE_ADJ, max = processRecord.originalMaxAdj)
        )
    }

    override fun computeAdj(processRecord: ProcessRecord, adj: Int): Int {
        return computeAdjByAdjHandleType(processRecord, adj)
    }

    override fun computeAdjByAdjHandleType(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        return when (processRecord.adjHandleActionType) {
            AdjHandleActionType.CUSTOM_MAIN_PROCESS -> {
                doCustomMainProcessAdj(
                    processRecord = processRecord,
                    adj = adj,
                    isUserSpaceAdj = isUserSpaceAdj,
                    isHighPriorityProcess = isHighPriorityProcess
                )
            }

            AdjHandleActionType.CUSTOM_SUBPROCESS -> {
                doCustomSubprocessAdj(
                    processRecord = processRecord,
                    adj = adj,
                    isUserSpaceAdj = isUserSpaceAdj,
                    isHighPriorityProcess = isHighPriorityProcess
                )
            }

            AdjHandleActionType.GLOBAL_OOM_ADJ -> {
                doGlobalOomScoreAdj(
                    processRecord = processRecord,
                    adj = adj,
                    isUserSpaceAdj = isUserSpaceAdj,
                    isHighPriorityProcess = isHighPriorityProcess
                )
            }

            else -> {
                doOther(
                    processRecord = processRecord,
                    adj = adj,
                    isUserSpaceAdj = isUserSpaceAdj,
                    isHighPriorityProcess = isHighPriorityProcess
                )
            }
        }
    }

    /**
     * 自定义了主进程adj
     */
    override fun doCustomMainProcessAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        val appOptimizePolicy = HookCommonProperties.getAppOptimizePolicy(
            userId = processRecord.userId,
            packageName = processRecord.packageName
        )
        val appInfo = processRecord.appInfo
        val appGroupEnum = appInfo.appGroupEnum
        val possibleAdj = when (appGroupEnum) {
            AppGroupEnum.ACTIVE -> appOptimizePolicy.getCustomMainProcessFgAdj()
            AppGroupEnum.IDLE -> appOptimizePolicy.getCustomMainProcessBgAdj()
            else -> null
        } ?: run {
            return doOther(
                processRecord = processRecord,
                adj = adj,
                isUserSpaceAdj = isUserSpaceAdj,
                isHighPriorityProcess = isHighPriorityProcess,
                appInfo = appInfo,
                appGroupEnum = appGroupEnum
            )
        }
        return computeHighPriorityProcessAdjByAdjHandlePolicy(
            processRecord = processRecord,
            adj = adj,
            appInfo = appInfo,
            adjHandleFunction = appInfo.adjHandleFunction
        ) { possibleAdj }
    }

    override fun doCustomSubprocessAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        val subProcessOomPolicy = HookCommonProperties.getSubProcessOomPolicy(
            userId = processRecord.userId,
            processName = processRecord.processName
        )
        val appInfo = processRecord.appInfo
        val appGroupEnum = appInfo.appGroupEnum
        val possibleAdj = when (appGroupEnum) {
            AppGroupEnum.ACTIVE -> subProcessOomPolicy.getCustomFgAdj()
            AppGroupEnum.IDLE -> subProcessOomPolicy.getCustomBgAdj()
            else -> null
        } ?: run {
            return doOther(
                processRecord = processRecord,
                adj = adj,
                isUserSpaceAdj = isUserSpaceAdj,
                isHighPriorityProcess = isHighPriorityProcess,
                appInfo = appInfo,
                appGroupEnum = appGroupEnum
            )
        }
        return computeHighPriorityProcessAdjByAdjHandlePolicy(
            processRecord = processRecord,
            adj = adj,
            appInfo = appInfo,
            adjHandleFunction = appInfo.adjHandleFunction
        ) { possibleAdj }
    }

    override fun doGlobalOomScoreAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
        appInfo: AppInfo,
        appGroupEnum: AppGroupEnum,
    ): Int {
        return if (globalOomScoreAdjHandler.isShouldHandle(
                isMainProcess = processRecord.mainProcess,
                isUserSpaceAdj = adj >= 0,
                isHighPriorityProcess = isHighPriorityProcess
            )
        ) {
            customGlobalOomScore
        } else {
            doOther(
                processRecord = processRecord,
                adj = adj,
                isUserSpaceAdj = isUserSpaceAdj,
                isHighPriorityProcess = isHighPriorityProcess,
                appInfo = appInfo,
                appGroupEnum = appGroupEnum
            )
        }
    }

    override fun doOther(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
        appInfo: AppInfo,
        appGroupEnum: AppGroupEnum,
    ): Int {
        if (!isUserSpaceAdj) {
            return adj
        }

        var finalApplyAdj: Int = adj
        // 高优先级进程
        if (isHighPriorityProcess) {
            when {
                appGroupEnum == AppGroupEnum.ACTIVE -> {
                    finalApplyAdj = computeHighPriorityProcessAdjInActiveGroup(processRecord, adj)
                }

                else -> {
                    finalApplyAdj = computeHighPriorityProcessAdjByAdjHandlePolicy(
                        processRecord = processRecord,
                        adj = adj,
                        adjHandleFunction = appInfo.adjHandleFunction,
                        appInfo = appInfo
                    ) {
                        computeHighPriorityProcessPossibleAdj(processRecord, adj, appInfo)
                    }
                }
            }
        } else { // 普通子进程
            finalApplyAdj = computeSubprocessAdj(
                processRecord = processRecord,
                adj = adj
            )
        }

        return finalApplyAdj
    }

    override fun checkAndSetDefaultMaxAdjIfNeed(processRecord: ProcessRecord) {
        processRecord.checkAndSetDefaultMaxAdjIfNeed()
    }

    override fun computeSubprocessAdj(
        processRecord: ProcessRecord,
        adj: Int,
    ): Int {
        var possibleFinalAdj = adj
        if (processRecord.fixedOomAdjScore != ProcessRecord.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
            val expectedOomAdjScore = ProcessRecord.SUB_PROC_ADJ
            possibleFinalAdj = if (adj > expectedOomAdjScore) {
                adj
            } else {
                expectedOomAdjScore
            }

            processRecord.fixedOomAdjScore = ProcessRecord.SUB_PROC_ADJ
            // 如果修改过maxAdj则重置
            processRecord.resetMaxAdj()
        } else if (adj < processRecord.fixedOomAdjScore) {    // 新的oomAdj小于已记录的子进程最小adj
            possibleFinalAdj = processRecord.fixedOomAdjScore
        }

        return possibleFinalAdj
    }

    private inline fun computeHighPriorityProcessAdjByAdjHandlePolicy(
        processRecord: ProcessRecord,
        adj: Int,
        noinline adjHandleFunction: (AppInfo) -> Boolean,
        appInfo: AppInfo = processRecord.appInfo,
        /**
         * 参数为[adj]
         */
        possibleAdjComputeBlock: (Int) -> Int,
    ): Int {
        return when (adjHandleFunction) {
            AppInfo.handleAdjNever -> adj
            AppInfo.handleAdjAlways -> {
                checkAndSetDefaultMaxAdjIfNeed(processRecord)
                possibleAdjComputeBlock(adj)
            }

            else -> {
                if (appInfo.shouldHandleAdj()) {
                    checkAndSetDefaultMaxAdjIfNeed(processRecord)
                    possibleAdjComputeBlock(adj)
                } else {
                    computeHighPriorityProcessAdjNotHasActivity(adj)
                }
            }
        }
    }

    private fun computeHighPriorityProcessAdjNotHasActivity(curAdj: Int): Int {
        /*return highPriorityProcessNotHasActivityAdjCache.computeIfAbsent(curAdj) { _ ->
            max(curAdj, ProcessRecord.SUB_PROC_ADJ)
        }*/
        return max(curAdj, ProcessRecord.SUB_PROC_ADJ)
    }

    override fun computeHighPriorityProcessPossibleAdj(
        processRecord: ProcessRecord,
        adj: Int,
        appInfo: AppInfo,
    ): Int {
        // 大多数国内 app 的 webview 崩溃也会直接导致 app 崩溃, 因此一视同仁
        return if (processRecord.mainProcess || processRecord.webviewProcessProbable) {
            computeMainProcessAdj(adj)
        } else {
            computeHighPrioritySubprocessAdj(adj)
        }
    }

    override fun computeMainProcessAdj(adj: Int): Int {
        val index = adj + ADJ_OFFSET
        val cachedAdj = mainProcessAdjCache[index]
        if (index in 0 until ADJ_CACHE_SIZE) {
            if (cachedAdj != UNINITIALIZED_VALUE) {
                return cachedAdj.toInt()
            }
            val computed = clamp(adj / adjConvertFactor, userProcessMinAdj, userProcessMaxAdj)
            mainProcessAdjCache[index] = computed.toShort()
            return computed
        }

        return clamp(adj / adjConvertFactor, userProcessMinAdj, userProcessMaxAdj)
    }

    public override fun computeHighPrioritySubprocessAdj(adj: Int): Int {
        val index = adj + ADJ_OFFSET
        val cachedAdj = subprocessAdjCache[index]
        if (index in 0 until ADJ_CACHE_SIZE) {
            if (cachedAdj != UNINITIALIZED_VALUE) {
                return cachedAdj.toInt()
            }
            val computed = computeMainProcessAdj(adj) + highPrioritySubprocessAdjOffset
            subprocessAdjCache[index] = computed.toShort()
            return computed
        }

        return computeMainProcessAdj(adj) + highPrioritySubprocessAdjOffset
    }

    override fun computeHighPriorityProcessAdjInActiveGroup(
        processRecord: ProcessRecord,
        adj: Int,
    ): Int {
        checkAndSetDefaultMaxAdjIfNeed(processRecord)
        return ProcessRecord.DEFAULT_MAIN_ADJ
    }

    companion object {
        private const val MIN_ADJ = ProcessList.NATIVE_ADJ
        private const val MAX_ADJ = ProcessList.UNKNOWN_ADJ
        private const val ADJ_CACHE_SIZE = MAX_ADJ - MIN_ADJ + 1
        private const val ADJ_OFFSET = -MIN_ADJ
        private const val UNINITIALIZED_VALUE = Short.MIN_VALUE

        /**
         * adj设置任务的优先级。
         *
         * 该值越大, 则任务优先级越高
         */
        const val ADJ_TASK_PRIORITY_LOWER = 1
        const val ADJ_TASK_PRIORITY_NORMAL = 5
        const val ADJ_TASK_PRIORITY_HIGHER = 10

        // 处于后台的进程的起始adj
        const val BACKGROUND_ADJ_START = 1

        // 从系统的adj到模块adj转换所需的因子
        const val ADJ_CONVERT_FACTOR =
            ProcessList.UNKNOWN_ADJ / ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ

        // 高优先级子进程相对主进程的偏移量
        const val HIGH_PRIORITY_SUBPROCESS_ADJ_OFFSET =
            ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
    }
}

private class CachedByteBufferThreadFactory : ThreadFactory {
    private val threadNumber = AtomicInteger(1)

    override fun newThread(r: Runnable?): Thread {
        return Thread(r, generateThreadName()).apply {
            if (isDaemon) {
                setDaemon(false)
            }
            if (priority != Thread.NORM_PRIORITY) {
                setPriority(Thread.NORM_PRIORITY)
            }
        }
    }

    private fun generateThreadName(): String {
        return "${THREAD_FACTORY_NAME}-${THREAD_NAME}-${threadNumber.getAndIncrement()}"
    }

    companion object {
        const val THREAD_FACTORY_NAME = "CachedByteBufferThreadFactory"
        const val THREAD_NAME = "CachedByteBufferThread"

        @JvmStatic
        val byteBufferThreadLocal = object : ThreadLocal<ByteBuffer>() {
            override fun initialValue(): ByteBuffer = ProcessList.getByteBufferUsedToWriteLmkd()
        }
    }
}

/**
 * 全局OOM ADJ处理器接口
 */
abstract class GlobalOomScoreAdjHandler {
    init {
        logTag()?.let { tag ->
            logInfo("全局OOM处理器: ${tag}")
        }
    }

    abstract fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean,
    ): Boolean

    open fun logTag(): String? = null

    companion object {
        const val PROPERTY_LISTENER_KEY = "GlobalOomScoreAdjHandler"
    }
}

class DisabledGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean,
    ): Boolean {
        return false
    }

    override fun logTag(): String = "禁用"
}

class MainProcessGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean,
    ): Boolean {
        return isMainProcess && isUserSpaceAdj
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS.uiName
}

class AllGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean,
    ): Boolean {
        return true
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.ALL.uiName
}

class MainProcessAnyGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean,
    ): Boolean {
        return isMainProcess
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS_ANY.uiName
}

class MainAndSubProcessGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean,
    ): Boolean {
        return isUserSpaceAdj
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS.uiName
}


/**
 * 是否升级子进程的等级
 * @receiver ProcessRecordKt
 * @return Boolean 升级 -> true
 */
fun ProcessRecord.isUpgradeSubProcessLevel(): Boolean =
    HookCommonProperties.isUpgradeSubProcessLevel(userId, processName)

/**
 * 是否需要处理webview进程
 * @receiver ProcessRecordKt
 * @return Boolean 需要处理 -> true
 */
fun ProcessRecord.isNeedHandleWebviewProcess(): Boolean {
    return HookCommonProperties.enableWebviewProcessProtect.value
            && this.webviewProcessProbable
            && this.mWindowProcessController.mHasClientActivities
}

/**
 * 是否是高优先级子进程
 * @receiver ProcessRecordKt
 * @return Boolean 高优先级 -> true
 */
fun ProcessRecord.isHighPrioritySubProcess(): Boolean {
    return isHighPrioritySubProcessByBasicProperty()
            || hasWakeLock()
}

fun ProcessRecord.isHighPrioritySubProcessByBasicProperty(): Boolean {
    return isUpgradeSubProcessLevel()
            || isNeedHandleWebviewProcess()
}

/**
 * 是否是高优先级进程
 * @receiver ProcessRecordKt
 * @return Boolean 高优先级 -> true
 */
fun ProcessRecord.isHighPriorityProcess(): Boolean = mainProcess || isHighPrioritySubProcess()

fun ProcessRecord.isHighPriorityProcessByBasicProperty(): Boolean {
    return mainProcess || isHighPrioritySubProcessByBasicProperty()
}

fun ProcessRecord.checkAndSetDefaultMaxAdjIfNeed() {
    if (fixedOomAdjScore != ProcessRecord.DEFAULT_MAIN_ADJ) {
        fixedOomAdjScore = ProcessRecord.DEFAULT_MAIN_ADJ

        if (ProcessRecord.isNeedSetDefaultMaxAdj) {
            setDefaultMaxAdj()
        }
    }
}
