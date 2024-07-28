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
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.log.logInfo
import com.venus.backgroundopt.xposed.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessList
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord.AdjHandleActionType
import com.venus.backgroundopt.xposed.entity.self.AppInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.manager.message.handle.getCustomMainProcessBgAdj
import com.venus.backgroundopt.xposed.manager.message.handle.getCustomMainProcessFgAdj
import com.venus.backgroundopt.xposed.util.getBooleanFieldValue
import com.venus.backgroundopt.xposed.util.getObjectFieldValue
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
) {
    /* *************************************************************************
     *                                                                         *
     * ADJ应用任务调度                                                           *
     *                                                                         *
     **************************************************************************/
    private val adjTaskPool = ExecutorUtils.newScheduleThreadPool(
        coreSize = 3,
        threadFactory = CachedByteBufferThreadFactory(),
        removeOnCancelPolicy = true
    )

    private val adjTaskMap = ConcurrentHashMap<ProcessRecord, ScheduledFuture<*>>(4, 1.5F)
    private val adjTaskPriorityMap = HashMap<ProcessRecord, Int>(4, 1.5F)

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
            val lastTaskPriority = adjTaskPriorityMap[processRecord]
            if (lastTaskPriority == null || priority >= lastTaskPriority) {
                lastScheduledFuture?.cancel(true)
                scheduleAdjTask {
                    adjTaskMap.remove(processRecord)
                    block()
                }
            } else {
                lastScheduledFuture
            }
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
    private val threadLocalMap = CachedByteBufferThreadFactory.threadLocalMap

    protected val highPriorityProcessNotHasActivityAdjMap = ConcurrentHashMap<Int, Int>(4)
    protected val mainProcessAdjMap = ConcurrentHashMap<Int, Int>(4)
    protected val highPrioritySubprocessAdjMap = ConcurrentHashMap<Int, Int>(4)
    protected val subprocessAdjMap = ConcurrentHashMap<Int, Int>(4)

    private val globalOomScorePolicy = HookCommonProperties.globalOomScorePolicy
    private var customGlobalOomScore = globalOomScorePolicy.value.customGlobalOomScore

    // 全局oom分数处理器
    @Volatile
    private var globalOomScoreAdjHandler: GlobalOomScoreAdjHandler = getGlobalOomScoreAdjHandler(
        globalOomScorePolicy.value
    ).also {
        globalOomScorePolicy.addListener(GlobalOomScoreAdjHandler.PROPERTY_LISTENER_KEY) { _, newValue ->
            OomAdjustManager.printAdjHandleActionTypeLog("全局OOM切换")
            globalOomScoreAdjHandler = getGlobalOomScoreAdjHandler(newValue)

            customGlobalOomScore = newValue.customGlobalOomScore

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

    open fun getAdjWillSet(processRecord: ProcessRecord, adj: Int): Int {
        return processRecord.processStateRecord.curRawAdj
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

    /**
     * 应用给定的adj
     */
    protected fun applyAdj(pid: Int, uid: Int, adj: Int) {
        ProcessList.writeLmkd(pid, uid, adj)
    }

    protected fun applyAdjUseCachedByteBuffer(pid: Int, uid: Int, adj: Int) {
        val threadLocal = threadLocalMap[Thread.currentThread()]!!
        val byteBuffer = threadLocal.get()!!
        ProcessList.writeLmkd(byteBuffer, pid, uid, adj)
        byteBuffer.clear()
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

    /**
     * 计算adj
     */
    protected open fun computeAdj(processRecord: ProcessRecord, adj: Int): Int {
        return computeAdjByAdjHandleType(processRecord, adj)
    }

    protected fun computeAdjByAdjHandleType(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean = adj >= 0,
        isHighPriorityProcess: Boolean = processRecord.isHighPriorityProcess(),
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
    protected open fun doCustomMainProcessAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        val appOptimizePolicy = HookCommonProperties.appOptimizePolicyMap[processRecord.packageName]
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

    protected open fun doCustomSubprocessAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        val subProcessOomPolicy =
            HookCommonProperties.subProcessOomPolicyMap[processRecord.processName]
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

    protected open fun doGlobalOomScoreAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
        appInfo: AppInfo = processRecord.appInfo,
        appGroupEnum: AppGroupEnum = appInfo.appGroupEnum,
    ): Int {
        return if (globalOomScoreAdjHandler.isShouldHandle(
                isMainProcess = processRecord.mainProcess,
                isUserSpaceAdj = adj >= 0,
                isHighPriorityProcess = isHighPriorityProcess
            )
        ) {
            computeHighPriorityProcessAdjByAdjHandlePolicy(
                processRecord = processRecord,
                adj = adj,
                adjHandleFunction = appInfo.adjHandleFunction,
                appInfo = appInfo
            ) {
                customGlobalOomScore
            }
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

    /**
     * 所有进程默认的处理方式
     */
    protected open fun doOther(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
        appInfo: AppInfo = processRecord.appInfo,
        appGroupEnum: AppGroupEnum = appInfo.appGroupEnum,
    ): Int {
        if (!isUserSpaceAdj) {
            return adj
        }

        var finalApplyAdj: Int = adj
        // 高优先级进程
        if (isHighPriorityProcess) {
            when {
                appGroupEnum == AppGroupEnum.ACTIVE -> {
                    checkAndSetDefaultMaxAdjIfNeed(processRecord)
                    finalApplyAdj = ProcessRecord.DEFAULT_MAIN_ADJ
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

    /**
     * 检查并对原生进程的adj设置最大值
     * @param processRecord ProcessRecord
     */
    protected open fun checkAndSetDefaultMaxAdjIfNeed(processRecord: ProcessRecord) {
        processRecord.checkAndSetDefaultMaxAdjIfNeed()
    }

    /**
     * 计算子进程的oom分数
     */
    protected open fun computeSubprocessAdj(
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

    /**
     * 根据[AppInfo.adjHandleFunction]来计算高优先级进程的adj
     */
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

    /**
     * 高优先级进程此时没有显示过界面(非 前台 -> 后台)
     */
    private fun computeHighPriorityProcessAdjNotHasActivity(curAdj: Int): Int {
        return highPriorityProcessNotHasActivityAdjMap.computeIfAbsent(curAdj) { _ ->
            max(curAdj, ProcessRecord.SUB_PROC_ADJ)
        }
    }

    /**
     * 计算高优先级进程可能会被使用的adj
     */
    protected open fun computeHighPriorityProcessPossibleAdj(
        processRecord: ProcessRecord,
        adj: Int,
        appInfo: AppInfo = processRecord.appInfo,
    ): Int {
        return if (processRecord.mainProcess) {
            computeMainProcessAdj(adj)
        } else {
            computeHighPrioritySubprocessAdj(adj)
        }
    }

    protected open fun computeMainProcessAdj(adj: Int): Int {
        return mainProcessAdjMap.computeIfAbsent(adj) { _ ->
            clamp(adj / adjConvertFactor, userProcessMinAdj, userProcessMaxAdj)
        }
    }

    open fun computeHighPrioritySubprocessAdj(adj: Int): Int {
        return subprocessAdjMap.computeIfAbsent(adj) { _ ->
            computeMainProcessAdj(adj) + highPrioritySubprocessAdjOffset
        }
    }

    companion object {
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

            threadLocalMap[this] = object : ThreadLocal<ByteBuffer>() {
                override fun initialValue(): ByteBuffer = ProcessList.getByteBufferUsedToWriteLmkd()
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
        val threadLocalMap = ConcurrentHashMap<Thread, ThreadLocal<ByteBuffer>>(4, 1.5F)
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
        return isHighPriorityProcess && isUserSpaceAdj
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS.uiName
}


/**
 * 是否升级子进程的等级
 * @receiver ProcessRecordKt
 * @return Boolean 升级 -> true
 */
fun ProcessRecord.isUpgradeSubProcessLevel(): Boolean =
    HookCommonProperties.isUpgradeSubProcessLevel(processName)

/**
 * 是否需要处理webview进程
 * @receiver ProcessRecordKt
 * @return Boolean 需要处理 -> true
 */
fun ProcessRecord.isNeedHandleWebviewProcess(): Boolean {
    return HookCommonProperties.enableWebviewProcessProtect.value
            && this.webviewProcessProbable
            && this.originalInstance.getObjectFieldValue(
        fieldName = FieldConstants.mWindowProcessController
    )?.getBooleanFieldValue(fieldName = FieldConstants.mHasClientActivities) == true
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
