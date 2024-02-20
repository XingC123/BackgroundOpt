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
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtils
import com.venus.backgroundopt.utils.concurrent.lock
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
        val processedAppGroup = arrayOf(AppGroupEnum.NONE, AppGroupEnum.IDLE)

        const val MAX_ALLOWED_OOM_SCORE_ADJ = ProcessList.UNKNOWN_ADJ - 1

        const val minSimpleLmkOomScore = 0
        const val maxSimpleLmkOomScore = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
        const val simpleLmkConvertDivisor = MAX_ALLOWED_OOM_SCORE_ADJ / maxSimpleLmkOomScore
        const val minSimpleLmkOtherProcessOomScore = maxSimpleLmkOomScore + 1

        const val simpleLmkMaxAndMinOffset = maxSimpleLmkOomScore - minSimpleLmkOomScore
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

    private fun handleSetOomAdj(param: MethodHookParam) {
        val uid = param.args[1] as Int
        val runningInfo = runningInfo
//        val appInfo = runningInfo.computeRunningAppIfAbsent(uid) ?: return
        val appInfo = runningInfo.getRunningAppInfo(uid) ?: return

        val globalOomScorePolicy = CommonProperties.globalOomScorePolicy.value
        if (!globalOomScorePolicy.enabled) {
            // 若当前为重要系统app, 则检查是否有界面
            if (runningInfo.isImportantSystemApp(appInfo.userId, appInfo.packageName)
                && appInfo.findAppResult?.hasActivity != true
            ) {
                return
            }

            // 若app未进入后台, 则不进行设置
            if (appInfo.appGroupEnum !in processedAppGroup) {
                return
            }
        }

        val pid = param.args[0] as Int
        val oomAdjScore = param.args[2] as Int

        // 获取当前进程对象
        val process =
            appInfo.getProcess(pid) ?: return
        /*?: appInfo.addProcess(runningInfo.activityManagerService.getProcessRecord(pid))*/
        val mainProcess = process.mainProcess

        val globalOomScoreEffectiveScopeEnum = globalOomScorePolicy.globalOomScoreEffectiveScope
        val customGlobalOomScore = globalOomScorePolicy.customGlobalOomScore
        // 对所有进程应用oom修改
        if (globalOomScorePolicy.enabled && globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.ALL) {
            param.args[2] = customGlobalOomScore
            appInfo.modifyProcessRecord(pid, customGlobalOomScore)
        } else {
            if (oomAdjScore >= 0 || globalOomScorePolicy.enabled) {
                if (mainProcess
                    || isUpgradeSubProcessLevel(process.processName)
                    || (CommonProperties.enableWebviewProcessProtect.value && process.webviewProcess)
                ) {
                    if (CommonProperties.oomWorkModePref.oomMode != OomWorkModePref.MODE_NEGATIVE
                        || globalOomScorePolicy.enabled
                    ) {
                        val useSimpleLmk =
                            CommonProperties.enableSimpleLmk.value /*&& (oomAdjScore in minSimpleLmkOomScore..maxSimpleLmkOomScore)*/
                        val appOptimizePolicy =
                            CommonProperties.appOptimizePolicyMap[process.packageName]

                        var possibleFinalAdj = appOptimizePolicy.getCustomMainProcessOomScore()
                        val finalAdj =
                            if (globalOomScorePolicy.enabled) {
                                // 进程独立配置优先于全局oom
                                possibleFinalAdj ?: customGlobalOomScore
                            } else
                            // simple lmk 只在平衡模式生效
                                if (useSimpleLmk && CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_BALANCE) {
                                    possibleFinalAdj = possibleFinalAdj
                                        ?: simpleLmkOomScoreMap.computeIfAbsent(oomAdjScore) { oomAdjScore / simpleLmkConvertDivisor }
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
                                        (possibleFinalAdj + simpleLmkMaxAndMinOffset).coerceAtLeast(
                                            minSimpleLmkOtherProcessOomScore
                                        )
                                    }
                                } else {
                                    possibleFinalAdj =
                                        possibleFinalAdj ?: ProcessRecordKt.DEFAULT_MAIN_ADJ
                                    if (mainProcess) {
                                        possibleFinalAdj
                                    } else {
                                        // 子进程升级、webview进程都默认比主进程adj大
                                        (possibleFinalAdj + 1).coerceAtMost(ProcessList.VISIBLE_APP_ADJ)
                                    }
                                }

                        if (!useSimpleLmk && process.fixedOomAdjScore != finalAdj) {
                            process.fixedOomAdjScore = finalAdj

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

                        param.args[2] = finalAdj
                    }

                    // 存入系统设置的oom_score_adj
                    appInfo.modifyProcessRecord(pid, oomAdjScore)
                } else { // 子进程的处理
                    if (globalOomScorePolicy.enabled && globalOomScoreEffectiveScopeEnum == GlobalOomScoreEffectiveScopeEnum.MAIN_AND_SUB_PROCESS) {
                        param.args[2] = customGlobalOomScore
                        appInfo.modifyProcessRecord(pid, oomAdjScore)
                    } else if (process.fixedOomAdjScore != ProcessRecordKt.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
                        val expectedOomAdjScore = ProcessRecordKt.SUB_PROC_ADJ
                        val finalOomAdjScore = if (oomAdjScore > expectedOomAdjScore) {
                            oomAdjScore
                        } else {
                            param.args[2] = expectedOomAdjScore
                            expectedOomAdjScore
                        }

                        process.oomAdjScore = finalOomAdjScore
                        process.fixedOomAdjScore = expectedOomAdjScore

                        // 如果修改过maxAdj则重置
                        process.resetMaxAdj()

                        if (BuildConfig.DEBUG) {
                            logProcessOomChanged(
                                appInfo.packageName,
                                uid,
                                pid,
                                mainProcess,
                                finalOomAdjScore
                            )
                        }
                    } else {
                        // 新的oomAdj小于修正过的adj 或 修正过的adj为不可能取值
                        if (oomAdjScore < process.fixedOomAdjScore) {
                            param.result = null
                        }
                        appInfo.modifyProcessRecord(pid, oomAdjScore)
                    }
                }

                // 修改curAdj
                process.processStateRecord.curAdj = param.args[2] as Int
            }
        }

        // 有的app启动后并未拥有过界面, 因此在其设置oom的时候将其送入后台分组
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
    }

    @Deprecated("ProcessRecordKt.getMDyingPid(proc)有时候为0")
    fun handleRemoveLruProcessLocked(param: MethodHookParam) {
        val proc = param.args[0]
        val uid = ProcessRecordKt.getUID(proc)
        val appInfo = runningInfo.getRunningAppInfo(uid) ?: return
        val pid = ProcessRecordKt.getMDyingPid(proc)

        runningInfo.removeProcess(appInfo, uid, pid)
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
        if (uid < ActivityManagerService.USER_APP_UID_START_NUM) {
            return
        }

        val pid = param.args[1] as Int
        val userId = ProcessRecordKt.getUserId(proc)
        val packageName = ProcessRecordKt.getPkgName(proc)
        runningInfo.startProcess(proc, uid, userId, packageName, pid)
    }
}