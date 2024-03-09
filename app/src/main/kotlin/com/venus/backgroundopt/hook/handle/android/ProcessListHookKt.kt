package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.core.RunningInfo.AppGroupEnum
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
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtils
import com.venus.backgroundopt.utils.concurrent.lock
import com.venus.backgroundopt.utils.getBooleanFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.utils.message.handle.getCustomMainProcessOomScore
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.util.concurrent.ConcurrentHashMap

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

        // 高水平的子进程的adj分数相对于主进程的偏移量
        const val highLevelSubProcessAdjOffset = 1
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

    /**
     * 是否升级子进程的等级
     *
     * @param processName 进程名
     * @return 升级 -> true
     */
    private fun isUpgradeSubProcessLevel(processName: String): Boolean =
        CommonProperties.subProcessOomPolicyMap[processName]?.let {
            it.policyEnum == SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
        } ?: false

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

    val simpleLmkOomScoreMap = ConcurrentHashMap<Int, Int>(8)
    val simpleLmkImportSystemAppOomScoreMap = ConcurrentHashMap<Int, Int>(8)

    private fun computeOomScoreAdjValueUseSimpleLmk(
        oomScoreAdj: Int
    ): Int = simpleLmkOomScoreMap.computeIfAbsent(oomScoreAdj) {
        (oomScoreAdj / simpleLmkConvertDivisor).coerceAtLeast(normalAppAdjStartUseSimpleLmk)
    }

    private fun getImportSystemAppOomScoreUseSimpleLmk(
        oomScoreAdj: Int
    ): Int = simpleLmkImportSystemAppOomScoreMap.computeIfAbsent(oomScoreAdj) { _ ->
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

    /**
     * 是否需要处理webview进程
     * @param processRecord ProcessRecordKt
     * @return Boolean 需要处理 -> true
     */
    private fun isNeedHandleWebviewProcess(processRecord: ProcessRecordKt): Boolean =
        CommonProperties.enableWebviewProcessProtect.value
                && processRecord.webviewProcessProbable
                && processRecord.processRecord.getObjectFieldValue(
            fieldName = FieldConstants.mWindowProcessController
        )?.getBooleanFieldValue(fieldName = FieldConstants.mHasClientActivities) == true

    /**
     * 是否是子进程oom高调节水平
     * @param processRecord ProcessRecordKt 进程记录器
     * @return Boolean 高水平 -> true
     */
    private fun isHighLevelSubProcess(
        processRecord: ProcessRecordKt
    ): Boolean = isUpgradeSubProcessLevel(processRecord.processName)
            || isNeedHandleWebviewProcess(processRecord)
            || processRecord.hasWakeLock()

    /**
     * simple lmk 只在平衡模式生效
     * @return Boolean 启用 -> true
     */
    private fun useSimpleLmk(): Boolean =
        CommonProperties.enableSimpleLmk.value && CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_BALANCE

    private fun handleSetOomAdj(param: MethodHookParam) {
        val pid = param.args[0] as Int
        val runningInfo = runningInfo
        // 获取当前进程对象
        val process = runningInfo.getRunningProcess(pid) ?: return
        val appInfo = process.appInfo
        var isImportantSystemApp = false

        val globalOomScorePolicy = CommonProperties.globalOomScorePolicy.value
        if (!globalOomScorePolicy.enabled) {
            isImportantSystemApp = appInfo.isImportSystemApp
            // 若当前为重要系统app, 则检查是否有界面
            if (isImportantSystemApp && appInfo.findAppResult?.hasActivity != true) {
                return
            }

            // 若app未进入后台, 则不进行设置
            if (appInfo.appGroupEnum !in processedAppGroup) {
                return
            }
        }
        val uid = param.args[1] as Int
        val oomAdjScore = param.args[2] as Int

        val mainProcess = process.mainProcess
        val lastOomScoreAdj = process.oomAdjScore
        var oomAdjustLevel = OomAdjustLevel.NONE
        // 最终要被系统设置的oom分数
        var finalApplyOomScoreAdj = oomAdjScore

        val globalOomScoreEffectiveScopeEnum = globalOomScorePolicy.globalOomScoreEffectiveScope
        val customGlobalOomScore = globalOomScorePolicy.customGlobalOomScore
        if (appInfo.appGroupEnum == AppGroupEnum.DEAD) {
            // app已经死亡
            // 24.3.7的逻辑是: 主进程被移除, 则会移除AppInfo。但是这个移除行为是在线程池完成的。
            // 因此, 与当前的逻辑是有一个线程安全问题的。
            // 所以我们在这里再确认下是否AppInfo已经被标记为死亡
            return
        } else if (globalOomScorePolicy.enabled && globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.ALL) {
            // 对所有进程应用oom修改
            finalApplyOomScoreAdj = customGlobalOomScore
        } else {
            val appOptimizePolicy = CommonProperties.appOptimizePolicyMap[process.packageName]
            var possibleFinalAdj = appOptimizePolicy.getCustomMainProcessOomScore()

            if (oomAdjScore >= 0 || possibleFinalAdj != null || globalOomScorePolicy.enabled) {
                if (mainProcess || isUpgradeSubProcessLevel(process.processName)
                    || isNeedHandleWebviewProcess(process)
                ) {
                    oomAdjustLevel = OomAdjustLevel.FIRST
                }

                if (oomAdjustLevel == OomAdjustLevel.FIRST) {
                    val useSimpleLmk = useSimpleLmk()
                    val finalAdj =
                        // 前台的oom_score_adj默认为0
                        if (appInfo.appGroupEnum == AppGroupEnum.ACTIVE) {
                            ProcessList.FOREGROUND_APP_ADJ
                        } else if (CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_NEGATIVE) {
                            possibleFinalAdj ?: oomAdjScore
                        } else if (globalOomScorePolicy.enabled) {
                            // 进程独立配置优先于全局oom
                            possibleFinalAdj ?: customGlobalOomScore
                        } else if (useSimpleLmk) {
                            possibleFinalAdj = possibleFinalAdj ?: if (isImportantSystemApp) {
                                getImportSystemAppOomScoreUseSimpleLmk(oomScoreAdj = oomAdjScore)
                            } else {
                                computeOomScoreAdjValueUseSimpleLmk(oomScoreAdj = oomAdjScore)
                            }

                            if (mainProcess) {
                                possibleFinalAdj
                            } else {
                                // 检查范围
                                /*val adj = possibleFinalAdj + simpleLmkMaxAndMinOffset
                                if (adj <= maxSimpleLmkOomScore) {
                                    minSimpleLmkOtherProcessOomScore
                                } else if (adj > ProcessList.VISIBLE_APP_ADJ) {
                                    ProcessList.VISIBLE_APP_ADJ
                                } else {
                                    adj
                                }*/
                                // 在当前minSimpleLmkOomScore = 0, maxSimpleLmkOomScore = 50,
                                // simpleLmkConvertDivisor = (ProcessList.UNKNOWN_ADJ - 1) / maxSimpleLmkOomScore
                                // 的情况下possibleFinalAdj + simpleLmkMaxAndMinOffset永远小于ProcessList.VISIBLE_APP_ADJ
                                // 2024.3.1: 此处最小为1 + 50 = 51.不用再比较判断了
                                (possibleFinalAdj + simpleLmkMaxAndMinOffset)/*.coerceAtLeast(
                                            minSimpleLmkOtherProcessOomScore
                                        )*/
                            }
                        } else {
                            possibleFinalAdj = getMainProcessOomScoreAdjNonNull(possibleFinalAdj)

                            if (mainProcess) {
                                possibleFinalAdj
                            } else {
                                // 子进程升级、webview进程都默认比主进程adj大
                                (possibleFinalAdj + highLevelSubProcessAdjOffset).coerceAtMost(
                                    ProcessList.VISIBLE_APP_ADJ
                                )
                            }
                        }

                    finalApplyOomScoreAdj = finalAdj
                    if (!useSimpleLmk && process.fixedOomAdjScore == Int.MIN_VALUE) {
                        val adj = getMainProcessOomScoreAdjNonNull(possibleFinalAdj)
                        process.fixedOomAdjScore = adj
                        finalApplyOomScoreAdj = adj

                        if (CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_STRICT ||
                            CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_NEGATIVE
                        ) {
                            process.setDefaultMaxAdj()
                        }

                        if (BuildConfig.DEBUG) {
                            logProcessOomChanged(
                                appInfo.packageName,
                                uid,
                                pid,
                                mainProcess,
                                finalAdj
                            )
                        }
                    }
                } else { // 子进程的处理
                    if (globalOomScorePolicy.enabled && globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS) {
                        finalApplyOomScoreAdj = customGlobalOomScore
                    } else if (process.hasWakeLock()) { // 拥有唤醒锁
                        finalApplyOomScoreAdj =
                            getMainProcessOomScoreAdjNonNull(possibleFinalAdj) + highLevelSubProcessAdjOffset
                    } else if (process.fixedOomAdjScore != ProcessRecordKt.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
                        val expectedOomAdjScore = ProcessRecordKt.SUB_PROC_ADJ
                        finalApplyOomScoreAdj = if (oomAdjScore > expectedOomAdjScore) {
                            oomAdjScore
                        } else {
                            expectedOomAdjScore
                        }

                        process.fixedOomAdjScore = expectedOomAdjScore

                        // 如果修改过maxAdj则重置
                        process.resetMaxAdj()

                        if (BuildConfig.DEBUG) {
                            logProcessOomChanged(
                                appInfo.packageName,
                                uid,
                                pid,
                                mainProcess,
                                finalApplyOomScoreAdj
                            )
                        }
                    } else {
                        // 新的oomAdj小于修正过的adj
                        if (oomAdjScore < process.fixedOomAdjScore) {
                            finalApplyOomScoreAdj = process.fixedOomAdjScore
                        }
                    }
                }
            }
        }

        param.args[2] = finalApplyOomScoreAdj
        // 修改curAdj
        process.processStateRecord.curAdj = finalApplyOomScoreAdj
        // 记录本次系统计算的分数
        process.oomAdjScore = oomAdjScore

        // ProcessListHookKt.handleHandleProcessStartedLocked 执行后, 生成进程所属的appInfo
        // 但并没有将其设置内存分组。若在进程创建时就设置appInfo的内存分组, 则在某些场景下会产生额外消耗。
        // 例如, 在打开新app时, 会首先创建进程, 紧接着显示界面。分别会执行: ①ProcessListHookKt.handleHandleProcessStartedLocked
        // ② ActivityManagerServiceHookKt.handleUpdateActivityUsageStats。
        // 此时, 若在①中设置了分组, 在②中会再次设置。即: 新打开app需要连续两次appInfo的内存分组迁移, 这是不必要的。
        // 我们的目标是保活以及额外处理, 那么只需在①中将其放入running.runningApps, 在设置oom时就可以被管理。
        // 此时那些没有打开过页面的app就可以被设置内存分组, 相应的进行内存优化处理。
        if (mainProcess) {
            ConcurrentUtils.execute(runningInfo.activityEventChangeExecutor, { throwable ->
                logger.error(
                    "检查App(userId: ${appInfo.userId}, 包名: ${appInfo.packageName}, uid: $uid" +
                            ")是否需要放置到idle分组出错: ${throwable.message}",
                    throwable
                )
            }) {
                appInfo.lock {
                    if (appInfo.appGroupEnum == AppGroupEnum.NONE) {
                        runningInfo.handleActivityEventChange(
                            ActivityManagerServiceHookKt.ACTIVITY_STOPPED,
                            null,
                            appInfo
                        )
                    }
                }
            }
        }

        // 内存压缩
        runningInfo.processManager.compactProcess(
            process,
            lastOomScoreAdj,
            oomAdjScore,
            oomAdjustLevel
        )
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