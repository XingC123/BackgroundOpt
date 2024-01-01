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
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtils
import com.venus.backgroundopt.utils.concurrent.lock
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

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

    private fun handleSetOomAdj(param: MethodHookParam) {
        val uid = param.args[1] as Int
        val runningInfo = runningInfo
//        val appInfo = runningInfo.computeRunningAppIfAbsent(uid) ?: return
        val appInfo = runningInfo.getRunningAppInfo(uid) ?: return
        // 若app未进入后台, 则不进行设置
        if (appInfo.appGroupEnum !in processedAppGroup) {
            return
        }

        val pid = param.args[0] as Int
        val oomAdjScore = param.args[2] as Int

        // 获取当前进程对象
        val process =
            appInfo.getProcess(pid) ?: return
        /*?: appInfo.addProcess(runningInfo.activityManagerService.getProcessRecord(pid))*/
        val mainProcess = process.mainProcess

        if (mainProcess || isUpgradeSubProcessLevel(process.processName)) { // 主进程
            // 获取自定义主进程oom分数
            val appOptimizePolicy = CommonProperties.appOptimizePolicyMap[process.packageName]
            val finalMainAdj = if (appOptimizePolicy?.enableCustomMainProcessOomScore == true) {
                if (appOptimizePolicy.customMainProcessOomScore == Int.MIN_VALUE) {
                    ProcessRecordKt.DEFAULT_MAIN_ADJ
                } else {
                    appOptimizePolicy.customMainProcessOomScore
                }
            } else {
                ProcessRecordKt.DEFAULT_MAIN_ADJ
            }

            if (process.fixedOomAdjScore != finalMainAdj) {
                process.oomAdjScore = oomAdjScore
                process.fixedOomAdjScore = finalMainAdj

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
                        finalMainAdj
                    )
                }
            } else {
                appInfo.modifyProcessRecord(pid, oomAdjScore)
            }

            // 修改实际的参数
            if (CommonProperties.oomWorkModePref.oomMode != OomWorkModePref.MODE_NEGATIVE) {
                param.args[2] = finalMainAdj
            }
        } else { // 子进程的处理
            if (process.fixedOomAdjScore != ProcessRecordKt.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
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