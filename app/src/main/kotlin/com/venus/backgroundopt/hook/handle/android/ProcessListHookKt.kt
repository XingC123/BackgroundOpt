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
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.afterHookAction
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getBooleanFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum
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
//        const val normalAppAdjStartUseSimpleLmk = importSystemAppAdjEndUseSimpleLmk + 1
        const val normalAppAdjStartUseSimpleLmk = minSimpleLmkOomScore + 1

        /**
         * 严格模式
         */
        const val minOomScoreAdj = -100
        const val maxOomScoreAdj = ProcessList.FOREGROUND_APP_ADJ
        const val maxAndMinOomScoreAdjDifference = maxOomScoreAdj - minOomScoreAdj
        const val oomScoreAdjConvertDivisor =
            MAX_ALLOWED_OOM_SCORE_ADJ / maxAndMinOomScoreAdjDifference

        // 高水平的子进程的adj分数相对于主进程的偏移量
        const val highLevelSubProcessAdjOffset = 1

        /* *************************************************************************
         *                                                                         *
         * oom_score_adj分数处理器                                                   *
         *                                                                         *
         **************************************************************************/
        const val normalMinAdj = /*-100*/ 0
        const val importAppMinAdj = /*-200*/0

        /**
         * 是否升级子进程的等级
         *
         * @param processName 进程名
         * @return 升级 -> true
         */
        @JvmStatic
        private fun isUpgradeSubProcessLevel(processName: String): Boolean =
            CommonProperties.subProcessOomPolicyMap[processName]?.let {
                it.policyEnum == SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
            } ?: false

        /**
         * 是否需要处理webview进程
         * @param processRecord ProcessRecordKt
         * @return Boolean 需要处理 -> true
         */
        @JvmStatic
        fun isNeedHandleWebviewProcess(processRecord: ProcessRecordKt): Boolean =
            CommonProperties.enableWebviewProcessProtect.value
                    && processRecord.webviewProcessProbable
                    && processRecord.originalInstance.getObjectFieldValue(
                fieldName = FieldConstants.mWindowProcessController
            )?.getBooleanFieldValue(fieldName = FieldConstants.mHasClientActivities) == true

        /**
         * 是否是高等级子进程
         * @param processRecord ProcessRecordKt 进程记录器
         * @return Boolean 高水平 -> true
         */
        @JvmStatic
        fun isHighLevelSubProcess(
            processRecord: ProcessRecordKt
        ): Boolean = isUpgradeSubProcessLevel(processRecord.processName)
                || isNeedHandleWebviewProcess(processRecord)
                || processRecord.hasWakeLock()

        /**
         * 是否是高等级进程
         * @param processRecord ProcessRecordKt
         * @return Boolean 高水平 -> true
         */
        @JvmStatic
        fun isHighLevelProcess(
            processRecord: ProcessRecordKt
        ): Boolean = processRecord.mainProcess || isHighLevelSubProcess(processRecord)
    }

    /* *************************************************************************
     *                                                                         *
     * oom adj处理器                                                            *
     *                                                                         *
     **************************************************************************/
    private val oomAdjHandler = when (CommonProperties.oomWorkModePref.oomMode) {
        OomWorkModePref.MODE_NEGATIVE -> object : OomScoreAdjHandler() {
            override fun computeFinalAdj(
                oomScoreAdj: Int,
                processRecord: ProcessRecordKt,
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
        minAdj = normalMinAdj,
        maxAdj = normalMinAdj + ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ,
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
    }.apply {
        highLevelSubProcessAdjOffset = maxAndMinAdjDifference
    }

    private fun generateStrictModeAdjHandler(): OomScoreAdjHandler = object : OomScoreAdjHandler(
        minAdj = normalMinAdj,
        maxAdj = normalMinAdj + ProcessList.FOREGROUND_APP_ADJ,
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
                Int::class.javaPrimitiveType    // oom_adj_score
            ),
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
        oomScoreAdj ?: ProcessRecordKt.DEFAULT_MAIN_ADJ

    private fun useSimpleLmk(): Boolean = CommonProperties.useSimpleLmk()

    private fun handleSetOomAdj(param: MethodHookParam) {
        val pid = param.args[0] as Int
        // 获取当前进程对象
        val process = runningInfo.getRunningProcess(pid) ?: return
        val appInfo = process.appInfo

        val globalOomScorePolicy = CommonProperties.globalOomScorePolicy.value
        if (!globalOomScorePolicy.enabled) {
            // 若app未进入后台, 则不进行设置
            if (appInfo.appGroupEnum !in processedAppGroup) {
                return
            }
        }

        if (appInfo.appGroupEnum == AppGroupEnum.DEAD) {
            // app已经死亡
            // 24.3.7的逻辑是: 主进程被移除, 则会移除AppInfo。但是这个移除行为是在线程池完成的。
            // 因此, 与当前的逻辑是有一个线程安全问题的。
            // 所以我们在这里再确认下是否AppInfo已经被标记为死亡
            // 24.3.25更正: 已弃用线程池异步方式, 但进行判断是更严谨的
            return
        }

        val mainProcess = process.mainProcess
        val lastSetRawAdj = process.mCurRawAdj
        var oomAdjustLevel = OomAdjustLevel.NONE

        val curRawAdj = process.processStateRecord.processStateRecord.callMethod<Int>(
            methodName = MethodConstants.getCurRawAdj
        )
        // 最终要被系统设置的oom分数
        var finalApplyOomScoreAdj = curRawAdj
        val globalOomScoreEffectiveScopeEnum = globalOomScorePolicy.globalOomScoreEffectiveScope
        val isHighLevelProcess = isHighLevelProcess(process).also {
            if (it) {
                oomAdjustLevel = OomAdjustLevel.FIRST
            }
        }
        val isUserSpaceAdj = curRawAdj >= 0

        val appOptimizePolicy = CommonProperties.appOptimizePolicyMap[process.packageName]
        val possibleAdj = appOptimizePolicy.getCustomMainProcessOomScore()

        if (appInfo.shouldHandleAdj()) {
            if (possibleAdj != null && isHighLevelProcess) {    // 进程独立配置优先于任何情况
                finalApplyOomScoreAdj = possibleAdj
                clearProcessUnexpectedState(processRecord = process)
            } else if (globalOomScorePolicy.enabled
                && (globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.ALL
                        || isHighLevelProcess && (globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS_ANY || isUserSpaceAdj)
                        || isUserSpaceAdj && globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS
                        )
            ) {
                // 逻辑概要:
                // 开启全局oom && (作用域 == ALL || isHighLevelProcess && (作用域 == MAIN_PROC_ANY || isUserSpaceAdj) || /* 普通子进程 或 !isUserSpaceAdj */ isUserSpaceAdj && 作用域 == MAIN_AND_SUB_PROC)
                finalApplyOomScoreAdj = globalOomScorePolicy.customGlobalOomScore
                clearProcessUnexpectedState(processRecord = process)
            } else if (isUserSpaceAdj) {
                if (isHighLevelProcess) {
                    val oomMode = CommonProperties.oomWorkModePref.oomMode
                    if (appInfo.appGroupEnum == AppGroupEnum.ACTIVE) {
                        finalApplyOomScoreAdj = ProcessRecordKt.DEFAULT_MAIN_ADJ
                    } else if (CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_NEGATIVE) {
                        // do nothing
                    } else {
                        finalApplyOomScoreAdj = oomAdjHandler.computeFinalAdj(
                            oomScoreAdj = curRawAdj,
                            processRecord = process,
                            appInfo = appInfo,
                            mainProcess = mainProcess
                        )
                        if (process.fixedOomAdjScore != ProcessRecordKt.defaultMaxAdj) {
                            process.fixedOomAdjScore = ProcessRecordKt.defaultMaxAdj

                            if (oomMode == OomWorkModePref.MODE_STRICT || oomMode == OomWorkModePref.MODE_NEGATIVE) {
                                process.setDefaultMaxAdj()
                            }
                        }
                    }
                } else {    // 普通子进程
                    finalApplyOomScoreAdj = computeSubprocessFinalOomScore(
                        processRecord = process,
                        oomScoreAdj = curRawAdj
                    )
                }
            }

            param.args[2] = finalApplyOomScoreAdj
            if (finalApplyOomScoreAdj != curRawAdj) {
                // 修改curAdj
                process.processStateRecord.curAdj = finalApplyOomScoreAdj
            }
        }

        // 记录本次系统计算的分数
        process.oomAdjScore = curRawAdj

        // 修改mSetRawAdj
        process.mCurRawAdj = curRawAdj

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
            lastSetRawAdj,
            curRawAdj,
            oomAdjustLevel
        )
    }

    /**
     * 清除进程非预期的状态记录
     * @param processRecord ProcessRecordKt
     */
    private fun clearProcessUnexpectedState(processRecord: ProcessRecordKt) {
        processRecord.processStateRecord.apply {
            cached = false
            empty = false
        }
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
        processRecord: ProcessRecordKt,
        mainProcess: Boolean
    ): Int {
        val possibleFinalAdj = if (appInfo.isImportSystemApp) {
            // getImportSystemAppOomScoreUseSimpleLmk(oomScoreAdj = oomScoreAdj)
            computeImportSystemAppOomAdj(oomScoreAdj = oomScoreAdj)
        } else {
            computeOomScoreAdjValueUseSimpleLmk(oomScoreAdj = oomScoreAdj)
        }

        return if (mainProcess) {
            clearProcessUnexpectedState(processRecord = processRecord)
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
        processRecord: ProcessRecordKt,
        oomScoreAdj: Int
    ): Int {
        var possibleFinalAdj = oomScoreAdj
        if (processRecord.fixedOomAdjScore != ProcessRecordKt.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
            val expectedOomAdjScore = ProcessRecordKt.SUB_PROC_ADJ
            possibleFinalAdj = if (oomScoreAdj > expectedOomAdjScore) {
                oomScoreAdj
            } else {
                expectedOomAdjScore
            }

            processRecord.fixedOomAdjScore = ProcessRecordKt.SUB_PROC_ADJ
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
        val pid = ProcessRecordKt.getMDyingPid(proc)

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
        val uid = ProcessRecordKt.getUID(proc)
        /*if (ActivityManagerService.isUnsafeUid(uid)) {
            return
        }*/

        val pid = param.args[1] as Int
        val userId = ProcessRecordKt.getUserId(proc)
        val packageName = ProcessRecordKt.getPkgName(proc)
        runningInfo.startProcess(proc, uid, userId, packageName, pid)
    }
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
internal open class OomScoreAdjHandler {
    var maxAllowedOomScoreAdj = ProcessList.UNKNOWN_ADJ - 1

    // 默认的主进程adj
    var defaultMainAdj = ProcessRecordKt.DEFAULT_MAIN_ADJ

    // 高水平的子进程的adj分数相对于主进程的偏移量
    var highLevelSubProcessAdjOffset = 1

    /* *************************************************************************
     *                                                                         *
     * 构造方法                                                                  *
     *                                                                         *
     **************************************************************************/
    @JvmOverloads
    constructor(
        defaultMainAdj: Int = ProcessRecordKt.DEFAULT_MAIN_ADJ,
        highLevelSubProcessAdjOffset: Int = 1,
        minAdj: Int = 0,
        maxAdj: Int = minAdj,
        minImportAppAdj: Int = 0,
        maxImportAppAdj: Int = minAdj,
    ) {
        this.defaultMainAdj = defaultMainAdj
        this.highLevelSubProcessAdjOffset = highLevelSubProcessAdjOffset
        this.minAdj = minAdj
        this.maxAdj = maxAdj
        this.minImportAppAdj = minImportAppAdj
        this.maxImportAppAdj = maxImportAppAdj

        updateValue()
    }

    /* *************************************************************************
     *                                                                         *
     * 普通进程                                                                  *
     *                                                                         *
     **************************************************************************/
    var minAdj = 0
        set(value) {
            field = value
            updateAdjConvertDivisor()
        }
    var maxAdj = minAdj
        set(value) {
            field = value
            updateAdjConvertDivisor()
        }
    var startAdj = minAdj + 1
    var maxAndMinAdjDifference = max(maxAdj - minAdj, 1)
    var adjConvertDivisor = 1

    private fun updateAdjConvertDivisor() {
        maxAndMinAdjDifference = computeMaxAndMinAdjDifference(
            maxAdj = maxAdj,
            minAdj = minAdj
        )
        adjConvertDivisor = computeAdjConvertDivisor()
    }

    open fun computeAdjConvertDivisor(): Int {
        return maxAllowedOomScoreAdj / maxAndMinAdjDifference
    }

    open fun computeAdj(oomScoreAdj: Int): Int {
        return oomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
            max((oomScoreAdj / adjConvertDivisor) + minAdj, startAdj)
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 重要进程                                                                  *
     *                                                                         *
     **************************************************************************/
    var minImportAppAdj = 0
        set(value) {
            field = value
            updateImportAppAdjConvertDivisor()
        }
    var maxImportAppAdj = minAdj
        set(value) {
            field = value
            updateImportAppAdjConvertDivisor()
        }
    var importAppStartAdj = minImportAppAdj + 1
    var maxAndMinImportAppAdjDifference = max(maxImportAppAdj - minImportAppAdj, 1)
    var importAppAdjConvertDivisor = 1

    private fun updateImportAppAdjConvertDivisor() {
        maxAndMinImportAppAdjDifference = computeMaxAndMinAdjDifference(
            maxAdj = maxImportAppAdj,
            minAdj = minImportAppAdj
        )
        importAppAdjConvertDivisor = computeImportAppAdjConvertDivisor()
    }

    open fun computeImportAppAdjConvertDivisor(): Int {
        return maxAllowedOomScoreAdj / maxAndMinImportAppAdjDifference
    }

    open fun computeImportAppAdj(oomScoreAdj: Int): Int {
        return importAppOomScoreAdjMap.computeIfAbsent(oomScoreAdj) { _ ->
            max((oomScoreAdj / importAppAdjConvertDivisor) + minImportAppAdj, importAppStartAdj)
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
     * 部分字段修改后需要额外对部分变量进行更新
     */
    fun updateValue() {
        startAdj = minAdj + 1
        importAppStartAdj = minImportAppAdj + 1
    }

    /**
     * 清除进程非预期的状态记录
     * @param processRecord ProcessRecordKt
     */
    private fun clearProcessUnexpectedState(processRecord: ProcessRecordKt) {
        processRecord.processStateRecord.apply {
            cached = false
            empty = false
        }
    }

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
        processRecord: ProcessRecordKt,
        appInfo: AppInfo = processRecord.appInfo,
        mainProcess: Boolean = processRecord.mainProcess,
    ): Int {
        return if (mainProcess) {
            clearProcessUnexpectedState(processRecord = processRecord)

            if (appInfo.isImportSystemApp) {
                // 严格模式我们设置了maxAdj。因此使用oomScoreAdj不够精确
                computeImportAppAdj(oomScoreAdj = oomScoreAdj)
            } else {
                computeAdj(oomScoreAdj = oomScoreAdj)
            }
        } else {    // 子进程
            computeAdj(oomScoreAdj = oomScoreAdj) + highLevelSubProcessAdjOffset
        }
    }

    companion object {
        val oomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)
        val importAppOomScoreAdjMap = ConcurrentHashMap<Int, Int>(8)
    }
}
