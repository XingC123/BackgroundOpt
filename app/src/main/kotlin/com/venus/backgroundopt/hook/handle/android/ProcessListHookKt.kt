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

package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.preference.OomWorkModePref
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.hook.HookCommonProperties
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.afterHookAction
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManager
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.clamp
import com.venus.backgroundopt.utils.getBooleanFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.ifTrue
import com.venus.backgroundopt.utils.log.logInfo
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.utils.message.handle.GlobalOomScorePolicy
import com.venus.backgroundopt.utils.message.handle.getCustomMainProcessOomScore
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * @author XingC
 * @date 2023/9/22
 */
class ProcessListHookKt(
    classLoader: ClassLoader?,
    hookInfo: RunningInfo?
) : MethodHook(classLoader, hookInfo) {
    companion object {
        @JvmField
        val processedAppGroup = arrayOf(
            AppGroupEnum.NONE,
            AppGroupEnum.ACTIVE,
            AppGroupEnum.IDLE
        )

        const val MAX_ALLOWED_OOM_SCORE_ADJ = ProcessList.UNKNOWN_ADJ - 1

        /**
         * Simple Lmk
         */
        const val minSimpleLmkOomScore = 0
        const val maxSimpleLmkOomScore = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
        const val simpleLmkConvertDivisor = MAX_ALLOWED_OOM_SCORE_ADJ / maxSimpleLmkOomScore
        const val minSimpleLmkOtherProcessOomScore = maxSimpleLmkOomScore + 1

        const val simpleLmkMaxAndMinOffset = maxSimpleLmkOomScore - minSimpleLmkOomScore
        const val visibleAppAdjUseSimpleLmk = ProcessList.VISIBLE_APP_ADJ / simpleLmkConvertDivisor

        const val importSystemAppAdjStartUseSimpleLmk = 1
        const val importSystemAppAdjEndUseSimpleLmk = 3

        @JvmField
        val importSystemAppAdjNormalUseSimpleLmk = "%.0f".format(
            (importSystemAppAdjStartUseSimpleLmk + importSystemAppAdjEndUseSimpleLmk) / 2.0
        ).toInt()

        // 第一等级的app的adj的起始值
        const val normalAppAdjStartUseSimpleLmk = importSystemAppAdjEndUseSimpleLmk + 1

        /**
         * 严格模式
         */
        const val minOomScoreAdj = -100
        const val maxOomScoreAdj = ProcessList.FOREGROUND_APP_ADJ
        const val maxAndMinOomScoreAdjDifference = maxOomScoreAdj - minOomScoreAdj
        const val oomScoreAdjConvertDivisor =
            MAX_ALLOWED_OOM_SCORE_ADJ / maxAndMinOomScoreAdjDifference

        // 高优先级的子进程的adj分数相对于主进程的偏移量
        const val highPrioritySubProcessAdjOffset = 1

        /* *************************************************************************
         *                                                                         *
         * oom_score_adj分数处理器                                                   *
         *                                                                         *
         **************************************************************************/
        const val normalMinAdj = /*-100*/ 1
        const val importAppMinAdj = /*-200*/1
        const val ADJ_CONVERT_DIVISOR =
            ProcessList.UNKNOWN_ADJ / ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
    }

    /* *************************************************************************
     *                                                                         *
     * oom adj处理器                                                            *
     *                                                                         *
     **************************************************************************/
    val oomAdjHandler = when (HookCommonProperties.oomWorkModePref.oomMode) {
        /*
         * 严格模式所有adj始终为0
         * (24.5.29)宽容模式在后续处理时, 并不会应用此oomAdjHandler的逻辑
         */
        OomWorkModePref.MODE_STRICT,
        OomWorkModePref.MODE_NEGATIVE -> object : OomScoreAdjHandler() {
            override fun computeFinalAdj(
                oomScoreAdj: Int,
                processRecord: ProcessRecord,
                appInfo: AppInfo,
                mainProcess: Boolean
            ): Int = 0
        }

        else -> {
            if (useSimpleLmk()) {
                generateSimpleLmkAdjHandler()
            } else {
                generateStrictModeAdjHandler()
            }
        }
    }

    private fun generateSimpleLmkAdjHandler(): OomScoreAdjHandler = object : OomScoreAdjHandler(
        highPrioritySubProcessAdjOffset = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ,
        minAdj = normalAppAdjStartUseSimpleLmk,
        maxAdj = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ - 1,
        adjConvertDivisor = ADJ_CONVERT_DIVISOR,
        minImportAppAdj = importSystemAppAdjStartUseSimpleLmk,
        maxImportAppAdj = importSystemAppAdjEndUseSimpleLmk
    ) {
        val importAppNormalAdj = "%.0f".format(
            (minImportAppAdj + maxImportAppAdj) / 2.0
        ).toInt()

        override fun computeImportAppAdj(oomScoreAdj: Int): Int {
            return importAppOomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
                if (oomScoreAdj < ProcessList.VISIBLE_APP_ADJ) {
                    minImportAppAdj
                } else if (oomScoreAdj < ProcessList.CACHED_APP_MIN_ADJ) {
                    importAppNormalAdj
                } else {
                    maxImportAppAdj
                }
            }
        }
    }

    private fun generateStrictModeAdjHandler(): OomScoreAdjHandler = object : OomScoreAdjHandler(
        highPrioritySubProcessAdjOffset = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ,
        minAdj = normalMinAdj,
        maxAdj = normalMinAdj + ProcessList.FOREGROUND_APP_ADJ,
        adjConvertDivisor = ADJ_CONVERT_DIVISOR,
        minImportAppAdj = importAppMinAdj,
        maxImportAppAdj = normalMinAdj
    ) {
        override fun computeAdj(oomScoreAdj: Int): Int {
            return minAdj
        }

        override fun computeImportAppAdj(oomScoreAdj: Int): Int {
            return minImportAppAdj
        }
    }

    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.ProcessList,
                MethodConstants.setOomAdj,
                arrayOf(
                    beforeHookAction { handleSetOomAdj(it) }
                ),
                Int::class.javaPrimitiveType,   // pid
                Int::class.javaPrimitiveType,   // uid
                Int::class.javaPrimitiveType,    // oom_adj_score
                // 三星设备这里会多一个int型参数
                // 所以索性直接hook名为此的所有方法
            ).setHookAllMatchedMethod(true),
            /*generateMatchedMethodHookPoint(
                true,
                ClassConstants.ProcessList,
                MethodConstants.removeLruProcessLocked,
                arrayOf(
                    beforeHookAction { handleRemoveLruProcessLocked(it) }
                )
            ),*/
            HookPoint(
                ClassConstants.ProcessList,
                MethodConstants.handleProcessStartedLocked,
                arrayOf(
                    afterHookAction { handleHandleProcessStartedLocked(it) }
                ),
                ClassConstants.ProcessRecord,       // app
                Int::class.javaPrimitiveType,       // pid
                Boolean::class.javaPrimitiveType,   // usingWrapper
                Long::class.javaPrimitiveType,      // expectedStartSeq
                Boolean::class.javaPrimitiveType,   // procAttached
            ),
        )
    }

    private fun logProcessOomChanged(
        packageName: String,
        uid: Int,
        pid: Int,
        mainProcess: Boolean = false,
        curAdj: Int
    ) = logger.debug(
        "设置${if (mainProcess) "主" else "子"}进程: [${packageName}, uid: ${uid}] ->>> " +
                "pid: ${pid}, adj: $curAdj"
    )

    val oomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)
    val importSystemAppOomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)

    private fun computeOomScoreAdjValueUseSimpleLmk(
        oomScoreAdj: Int
    ): Int = oomScoreAdjMap.computeIfAbsent(oomScoreAdj) {
        (oomScoreAdj / simpleLmkConvertDivisor).coerceAtLeast(normalAppAdjStartUseSimpleLmk)
    }

    private fun getImportSystemAppOomScoreUseSimpleLmk(
        oomScoreAdj: Int
    ): Int = importSystemAppOomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
        if (oomScoreAdj < ProcessList.VISIBLE_APP_ADJ) {
            importSystemAppAdjStartUseSimpleLmk
        } else if (oomScoreAdj < ProcessList.CACHED_APP_MIN_ADJ) {
            importSystemAppAdjNormalUseSimpleLmk
        } else {
            importSystemAppAdjEndUseSimpleLmk
        }
    }

    private fun getMainProcessOomScoreAdjNonNull(oomScoreAdj: Int?): Int =
        oomScoreAdj ?: ProcessRecord.DEFAULT_MAIN_ADJ

    private fun useSimpleLmk(): Boolean = HookCommonProperties.useSimpleLmk

    /**
     * 应用最终要被应用的adj
     *
     * 当不满足一些条件时, [block]不会被执行
     * @param appInfo AppInfo app信息
     * @param appOptimizePolicy AppOptimizePolicy? app优化配置
     * @param block Function0<Unit> 在此方法内, 完成对adj的赋值
     */
    private inline fun applyHighPriorityProcessFinalAdj(
        appInfo: AppInfo,
        appOptimizePolicy: AppOptimizePolicy?,
        curAdj: Int,
        shouldNotHandleAdjBlock: (Int) -> Int = {
            if (appInfo.isImportSystemApp) {
                curAdj
            } else {
                max(it, ProcessRecord.SUB_PROC_ADJ)
            }
        },
        block: (Int) -> Int
    ): Int {
        return when (appInfo.shouldHandleAdj) {
            AppInfo.handleAdjAlways -> block(curAdj)
            AppInfo.handleAdjNever -> curAdj
            AppInfo.handleAdjIfHasActivity -> {
                if (appInfo.shouldHandleAdj()) {
                    block(curAdj)
                } else {
                    computeHighPriorityProcessAdjNotHasActivity(appInfo, curAdj)
                }
            }

            else -> computeHighPriorityProcessAdjNotHasActivity(appInfo, curAdj)
        }
    }

    private fun computeHighPriorityProcessAdjNotHasActivity(
        appInfo: AppInfo,
        curAdj: Int
    ): Int {
        return /*if (appInfo.isImportSystemApp) {
            curAdj
        } else {
            max(curAdj, ProcessRecord.SUB_PROC_ADJ)
        }*/ max(curAdj, ProcessRecord.SUB_PROC_ADJ)
    }

    /**
     * 全局oom分数处理器
     */
    @Volatile
    private var globalOomScoreAdjHandler: GlobalOomScoreAdjHandler = run {
        val globalOomScorePolicy = HookCommonProperties.globalOomScorePolicy
        val adjHandler = getGlobalOomScoreAdjHandler(globalOomScorePolicy.value)

        globalOomScorePolicy.addListener(GlobalOomScoreAdjHandler.PROPERTY_LISTENER_KEY) { _, newValue ->
            globalOomScoreAdjHandler = getGlobalOomScoreAdjHandler(newValue)
        }

        adjHandler
    }

    private fun getGlobalOomScoreAdjHandler(
        globalOomScorePolicy: GlobalOomScorePolicy
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

    private fun handleSetOomAdj(param: MethodHookParam) {
        val pid = param.args[0] as Int
        // 获取当前进程对象
        val process = runningInfo.getRunningProcess(pid) ?: return
        val appInfo = process.appInfo

        // app已死亡
        if (appInfo.appGroupEnum == AppGroupEnum.DEAD) {
            return
        }

        val globalOomScorePolicy = HookCommonProperties.globalOomScorePolicy.value
        if (!globalOomScorePolicy.enabled) {
            // 若app未进入后台, 则不进行设置
            /*if (appInfo.appGroupEnum !in processedAppGroup) {
                return
            }*/
            when (appInfo.appGroupEnum) {
                AppGroupEnum.NONE, AppGroupEnum.ACTIVE, AppGroupEnum.IDLE -> {
                    // 将会被处理
                }

                else -> return
            }
        }

        // 本次要设置的adj
        val adj = param.args[2] as Int
        val mainProcess = process.mainProcess
        val adjLastSet = process.oomAdjScore
        var oomAdjustLevel = OomAdjustLevel.NONE
        // 是否修改方法参数中的oom_score_adj
        var doHookOriginalAdj = true

        val oomMode = HookCommonProperties.oomWorkModePref.oomMode
        val isNegativeMode = oomMode == OomWorkModePref.MODE_NEGATIVE
        val adjWillSet = if (isNegativeMode) {
            adj
        } else {
            process.processStateRecord.curRawAdj
        }

        // 最终要被系统设置的oom分数
        var finalApplyOomScoreAdj = adjWillSet
        val isHighPriorityProcess = process.isHighPriorityProcess().ifTrue {
            oomAdjustLevel = OomAdjustLevel.FIRST
        }
        val isUserSpaceAdj = adjWillSet >= 0

        val appOptimizePolicy = HookCommonProperties.appOptimizePolicyMap[process.packageName]
        val possibleAdj = appOptimizePolicy.getCustomMainProcessOomScore()

        if (possibleAdj != null && isHighPriorityProcess) {    // 进程独立配置优先于任何情况
            finalApplyOomScoreAdj = applyHighPriorityProcessFinalAdj(
                appInfo = appInfo,
                appOptimizePolicy = appOptimizePolicy,
                curAdj = adjWillSet,
            ) {
                // process.clearProcessUnexpectedState()
                possibleAdj
            }
        } else if (globalOomScoreAdjHandler.isShouldHandle(
                isMainProcess = mainProcess,
                isUserSpaceAdj = isUserSpaceAdj,
                isHighPriorityProcess = isHighPriorityProcess
            )
        ) {
            finalApplyOomScoreAdj = applyHighPriorityProcessFinalAdj(
                appInfo = appInfo,
                appOptimizePolicy = appOptimizePolicy,
                curAdj = adjWillSet,
            ) {
                // process.clearProcessUnexpectedState()
                globalOomScorePolicy.customGlobalOomScore
            }
        } else if (isUserSpaceAdj) {
            if (appInfo.appGroupEnum == AppGroupEnum.ACTIVE) {
                finalApplyOomScoreAdj = adj
            } else if (isHighPriorityProcess) {
                if (isNegativeMode) {
                    doHookOriginalAdj = false
                } else {
                    finalApplyOomScoreAdj = applyHighPriorityProcessFinalAdj(
                        appInfo = appInfo,
                        appOptimizePolicy = appOptimizePolicy,
                        curAdj = adjWillSet
                    ) {
                        if (process.fixedOomAdjScore != ProcessRecord.DEFAULT_MAIN_ADJ) {
                            process.fixedOomAdjScore = ProcessRecord.DEFAULT_MAIN_ADJ

                            if (ProcessRecord.isNeedSetDefaultMaxAdj) {
                                process.setDefaultMaxAdj()
                            }
                        }
                        oomAdjHandler.computeFinalAdj(
                            oomScoreAdj = adjWillSet,
                            processRecord = process,
                            appInfo = appInfo,
                            mainProcess = mainProcess
                        )
                    }
                }
            } else {    // 普通子进程
                finalApplyOomScoreAdj = computeSubprocessFinalOomScore(
                    processRecord = process,
                    oomScoreAdj = adjWillSet
                )
            }
        } else {
            doHookOriginalAdj = false
        }

        if (doHookOriginalAdj) {
            param.result = null
            ProcessList.writeLmkd(
                pid,
                process.uid,
                finalApplyOomScoreAdj
            )
        }

        // 记录本次系统计算的分数
        process.oomAdjScore = adjWillSet

        // ProcessListHookKt.handleHandleProcessStartedLocked 执行后, 生成进程所属的appInfo
        // 但并没有将其设置内存分组。若在进程创建时就设置appInfo的内存分组, 则在某些场景下会产生额外消耗。
        // 例如, 在打开新app时, 会首先创建进程, 紧接着显示界面。分别会执行: ①ProcessListHookKt.handleHandleProcessStartedLocked
        // ② ActivityManagerServiceHookKt.handleUpdateActivityUsageStats。
        // 此时, 若在①中设置了分组, 在②中会再次设置。即: 新打开app需要连续两次appInfo的内存分组迁移, 这是不必要的。
        // 我们的目标是保活以及额外处理, 那么只需在①中将其放入running.runningApps, 在设置oom时就可以被管理。
        // 此时那些没有打开过页面的app就可以被设置内存分组, 相应的进行内存优化处理。
        if (mainProcess) {
            /*ConcurrentUtils.execute(runningInfo.activityEventChangeExecutor, { throwable ->
                logger.error(
                    "检查App(userId: ${appInfo.userId}, 包名: ${appInfo.packageName}, uid: $uid" +
                            ")是否需要放置到idle分组出错: ${throwable.message}",
                    throwable
                )
            }) {*/
            if (appInfo.appGroupEnum == AppGroupEnum.NONE) {
                runningInfo.handleActivityEventChange(
                    ActivityManagerServiceHookKt.ACTIVITY_STOPPED,
                    null,
                    appInfo
                )
                // }
            }
        }

        // 内存压缩
        runningInfo.processManager.compactProcess(
            process,
            adjLastSet,
            adjWillSet,
            oomAdjustLevel
        )
    }

    /**
     * 在使用Simple Lmk的情况下, 计算最终adj
     * @param appInfo AppInfo
     * @param oomScoreAdj Int
     * @param processRecord ProcessRecordKt
     * @param mainProcess Boolean
     * @return Int
     */
    private fun computeFinalOomScoreUseSimpleLmk(
        appInfo: AppInfo,
        oomScoreAdj: Int,
        processRecord: ProcessRecord,
        mainProcess: Boolean
    ): Int {
        val possibleFinalAdj = if (appInfo.isImportSystemApp) {
            // getImportSystemAppOomScoreUseSimpleLmk(oomScoreAdj = oomScoreAdj)
            computeImportSystemAppOomAdj(oomScoreAdj = oomScoreAdj)
        } else {
            computeOomScoreAdjValueUseSimpleLmk(oomScoreAdj = oomScoreAdj)
        }

        return if (mainProcess) {
            // processRecord.clearProcessUnexpectedState()
            possibleFinalAdj
        } else {
            possibleFinalAdj + simpleLmkMaxAndMinOffset
        }
    }

    /**
     * 计算子进程的oom分数
     * @param processRecord ProcessRecordKt
     * @param oomScoreAdj Int
     * @return Int
     */
    private fun computeSubprocessFinalOomScore(
        processRecord: ProcessRecord,
        oomScoreAdj: Int
    ): Int {
        var possibleFinalAdj = oomScoreAdj
        if (processRecord.fixedOomAdjScore != ProcessRecord.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
            val expectedOomAdjScore = ProcessRecord.SUB_PROC_ADJ
            possibleFinalAdj = if (oomScoreAdj > expectedOomAdjScore) {
                oomScoreAdj
            } else {
                expectedOomAdjScore
            }

            processRecord.fixedOomAdjScore = ProcessRecord.SUB_PROC_ADJ
            // 如果修改过maxAdj则重置
            processRecord.resetMaxAdj()
        } else if (oomScoreAdj < processRecord.fixedOomAdjScore) {    // 新的oomAdj小于已记录的子进程最小adj
            possibleFinalAdj = processRecord.fixedOomAdjScore
        }

        return possibleFinalAdj
    }

    /**
     * 计算系统app的adj分数
     * @param oomScoreAdj Int 当前系统计算的oom_score_adj
     * @return Int
     */
    private fun computeImportSystemAppOomAdj(oomScoreAdj: Int): Int {
        return importSystemAppOomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
            (oomScoreAdj / oomScoreAdjConvertDivisor) + minOomScoreAdj
        }
    }

    @Deprecated("ProcessRecordKt.getMDyingPid(proc)有时候为0")
    fun handleRemoveLruProcessLocked(param: MethodHookParam) {
        val proc = param.args[0]
        val pid = ProcessRecord.getMDyingPid(proc)

        runningInfo.removeProcess(pid)
    }

    /* *************************************************************************
     *                                                                         *
     * 进程新建                                                                  *
     *                                                                         *
     **************************************************************************/
    fun handleHandleProcessStartedLocked(param: MethodHookParam) {
        if (!(param.result as Boolean)) {
            return
        }

        val proc = param.args[0]
        val uid = ProcessRecord.getUID(proc)
        /*if (ActivityManagerService.isUnsafeUid(uid)) {
            return
        }*/

        val pid = param.args[1] as Int
        val userId = ProcessRecord.getUserId(proc)
        val packageName = ProcessRecord.getPkgName(proc)
        runningInfo.startProcess(proc, uid, userId, packageName, pid)
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
        isHighPriorityProcess: Boolean
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
        isHighPriorityProcess: Boolean
    ): Boolean {
        return false
    }

    override fun logTag(): String = "禁用"
}

class MainProcessGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean
    ): Boolean {
        return isMainProcess
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS.uiName
}

class AllGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean
    ): Boolean {
        return true
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.ALL.uiName
}

class MainProcessAnyGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean
    ): Boolean {
        return isHighPriorityProcess && isUserSpaceAdj
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS_ANY.uiName
}

class MainAndSubProcessGlobalOomScoreAdjHandler : GlobalOomScoreAdjHandler() {
    override fun isShouldHandle(
        isUserSpaceAdj: Boolean,
        isMainProcess: Boolean,
        isHighPriorityProcess: Boolean
    ): Boolean {
        return isUserSpaceAdj
    }

    override fun logTag(): String = GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS.uiName
}

/**
 * 进程OOM调整等级
 */
class OomAdjustLevel {
    companion object {
        const val NONE = 0
        const val FIRST = 1
        const val SECOND = 2
    }
}

/**
 * oom_score_adj的处理器
 */
open class OomScoreAdjHandler {
    var maxAllowedOomScoreAdj = ProcessList.UNKNOWN_ADJ - 1

    // 默认的主进程adj
    var defaultMainAdj = ProcessRecord.DEFAULT_MAIN_ADJ

    // 高优先级的子进程的adj分数相对于主进程的偏移量
    var highPrioritySubProcessAdjOffset = 1

    /* *************************************************************************
     *                                                                         *
     * 构造方法                                                                  *
     *                                                                         *
     **************************************************************************/
    @JvmOverloads
    constructor(
        defaultMainAdj: Int = ProcessRecord.DEFAULT_MAIN_ADJ,
        highPrioritySubProcessAdjOffset: Int = 1,
        minAdj: Int = 0,
        maxAdj: Int = minAdj,
        adjConvertDivisor: Int = 1,
        minImportAppAdj: Int = minAdj,
        maxImportAppAdj: Int = maxAdj,
        importAppAdjConvertDivisor: Int = adjConvertDivisor

    ) {
        this.defaultMainAdj = defaultMainAdj
        this.highPrioritySubProcessAdjOffset = highPrioritySubProcessAdjOffset

        this.minAdj = minAdj
        this.maxAdj = maxAdj
        this.adjConvertDivisor = adjConvertDivisor

        this.minImportAppAdj = minImportAppAdj
        this.maxImportAppAdj = maxImportAppAdj
        this.importAppAdjConvertDivisor = importAppAdjConvertDivisor
    }

    /* *************************************************************************
     *                                                                         *
     * 普通进程                                                                  *
     *                                                                         *
     **************************************************************************/
    var minAdj = 0
    var maxAdj = minAdj
    var adjConvertDivisor = 1

    open fun computeAdj(oomScoreAdj: Int): Int {
        return oomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
            clamp(oomScoreAdj / adjConvertDivisor, minAdj, maxAdj)
        }
    }

    /**
     * 计算高优先级子进程的adj
     * @param oomScoreAdj Int 当前系统的adj
     * @return Int 计算后的子进程adj
     */
    open fun computeHighPrioritySubProcessAdj(oomScoreAdj: Int): Int {
        return subProcessOomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
            computeAdj(oomScoreAdj = oomScoreAdj) + highPrioritySubProcessAdjOffset
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 重要进程                                                                  *
     *                                                                         *
     **************************************************************************/
    var minImportAppAdj = 0
    var maxImportAppAdj = minAdj
    var importAppAdjConvertDivisor = 1

    open fun computeImportAppAdj(oomScoreAdj: Int): Int {
        return importAppOomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
            clamp(oomScoreAdj / importAppAdjConvertDivisor, minImportAppAdj, maxImportAppAdj)
        }
    }

    /* *************************************************************************
     *                                                                         *
     * Common                                                                  *
     *                                                                         *
     **************************************************************************/
    /**
     * 计算最大与最小的adj的差值
     * @param maxAdj Int
     * @param minAdj Int
     * @return Int
     */
    open fun computeMaxAndMinAdjDifference(maxAdj: Int, minAdj: Int): Int = max(maxAdj - minAdj, 1)

    /**
     * 计算最终的分数
     * @param oomScoreAdj Int 系统计算的oom_score_adj
     * @param processRecord ProcessRecordKt 进程记录器的包装器
     * @param appInfo AppInfo 应用信息
     * @param mainProcess Boolean 是否是主进程
     * @return Int 计算后的最终分数
     */
    open fun computeFinalAdj(
        oomScoreAdj: Int,
        processRecord: ProcessRecord,
        appInfo: AppInfo = processRecord.appInfo,
        mainProcess: Boolean = processRecord.mainProcess,
    ): Int {
        return if (mainProcess) {
            // processRecord.clearProcessUnexpectedState()

            if (appInfo.isImportSystemApp) {
                // 严格模式我们设置了maxAdj。因此使用oomScoreAdj不够精确
                computeImportAppAdj(oomScoreAdj = oomScoreAdj)
            } else {
                computeAdj(oomScoreAdj = oomScoreAdj)
            }
        } else {    // 子进程
            computeHighPrioritySubProcessAdj(oomScoreAdj = oomScoreAdj)
        }
    }

    companion object {
        val oomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)
        val subProcessOomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)
        val importAppOomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)
    }
}

/**
 * 是否升级子进程的等级
 * @receiver ProcessRecordKt
 * @return Boolean 升级 -> true
 */
fun ProcessRecord.isUpgradeSubProcessLevel(): Boolean {
    return HookCommonProperties.subProcessOomPolicyMap[this.processName]?.let {
        it.policyEnum == SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
    } ?: false
}

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
    return isUpgradeSubProcessLevel()
            || isNeedHandleWebviewProcess()
            || hasWakeLock()
}

/**
 * 是否是高优先级进程
 * @receiver ProcessRecordKt
 * @return Boolean 高优先级 -> true
 */
fun ProcessRecord.isHighPriorityProcess(): Boolean = mainProcess || isHighPrioritySubProcess()

/**
 * 清除进程非预期的状态记录
 */
fun ProcessRecord.clearProcessUnexpectedState() {
    this.processStateRecord.apply {
        cached = false
        empty = false
        curProcState = ActivityManager.PROCESS_STATE_TOP
    }
}
