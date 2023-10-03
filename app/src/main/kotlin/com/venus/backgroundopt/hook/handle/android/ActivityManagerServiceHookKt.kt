package com.venus.backgroundopt.hook.handle.android

import android.content.Intent
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.afterHookAction
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.concurrent.lock
import com.venus.backgroundopt.utils.message.registeredMessageHandler
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/19
 */
class ActivityManagerServiceHookKt(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
//            HookPoint(
//                ClassConstants.ActivityManagerService,
//                MethodConstants.forceStopPackage,
//                arrayOf(
//                    afterHookAction {
//                        handleForceStopPackage(it)
//                    }
//                ),
//                String::class.java, /* packageName */
//                Int::class.java /* userId */
//            ),
            HookPoint(
                ClassConstants.ActivityManagerService,
                MethodConstants.cleanUpApplicationRecordLocked,
                arrayOf(
                    afterHookAction {
                        handleCleanUpApplicationRecordLocked(it)
                    }
                ),
                ClassConstants.ProcessRecord,
                Int::class.java,    /* pid */
                Boolean::class.java,    /* restarting */
                Boolean::class.java,    /* allowRestart */
                Int::class.java,    /* index */
                Boolean::class.java,    /* replacingPid */
                Boolean::class.java /* fromBinderDied */
            ),
            HookPoint(
                ClassConstants.ActivityManagerService,
                MethodConstants.killProcessesBelowAdj,
                arrayOf(
                    beforeHookAction {
                        handleKillProcessesBelowAdj(it)
                    }
                ),
                Int::class.java,    /* belowAdj */
                String::class.java  /* reason */
            ),
            HookPoint(
                ClassConstants.ActivityManagerService,
                "startService",
                arrayOf(
                    beforeHookAction { handleStartService(it) }
                ),
                ClassConstants.IApplicationThread,      // caller
                Intent::class.java,                     // service
                String::class.java,                     // resolvedType
                Boolean::class.java,                    // requireForeground
                String::class.java,                     // callingPackage
                String::class.java,                     // callingFeatureId
                Int::class.java                         // userId
            )
        )
    }

    /* *************************************************************************
     *                                                                         *
     * 停止app后台                                                               *
     *                                                                         *
     **************************************************************************/
    private fun handleForceStopPackage(param: MethodHookParam) {
        val packageName = param.args[0] as String
        val userId = param.args[1] as Int

        // 获取缓存的app的信息
        val normalAppResult = runningInfo.isNormalApp(userId, packageName)
        if (!normalAppResult.isNormalApp) {
            return
        }

        val uid = normalAppResult.applicationInfo.uid
        val appInfo = runningInfo.getRunningAppInfo(uid)
        appInfo?.let {
            runningInfo.removeRunningApp(appInfo)

            if (BuildConfig.DEBUG) {
                logger.debug("kill: ${appInfo.packageName}, uid: $uid")
            }
        }
    }

    private fun handleCleanUpApplicationRecordLocked(param: MethodHookParam) {
        val processRecord = param.args[0] as Any
        val uid = ProcessRecordKt.getUID(processRecord)
        val appInfo = runningInfo.getRunningAppInfo(uid)

        appInfo ?: return

        val pid = param.args[1] as Int
        val processRecordKt = appInfo.getProcess(pid)
        val mainProcess = processRecordKt?.mainProcess ?: false
        val packageName = appInfo.packageName

        if (mainProcess) {
            appInfo.lock {
                runningInfo.removeRunningApp(appInfo)
                if (BuildConfig.DEBUG) {
                    logger.debug("kill: ${packageName}, uid: $uid >>> 杀死App")
                }
            }
        } else {
            appInfo.lock {
                // 移除进程记录
                val process = appInfo.removeProcess(pid)
                // 取消进程的待压缩任务
                runningInfo.processManager.cancelCompactProcess(process)
                if (BuildConfig.DEBUG) {
                    logger.debug("kill: ${packageName}, uid: ${uid}, pid: $pid >>> 子进程被杀")
                }
            }
        }
    }

    private fun handleKillProcessesBelowAdj(param: MethodHookParam) {
        // 拔高adj分数
        param.args[0] = ProcessRecordKt.SUB_PROC_ADJ
    }

    /**
     * ui消息监听
     */
    private fun handleStartService(param: MethodHookParam) {
        val dataIntent = param.args[1] as Intent
        if (dataIntent.`package` != BuildConfig.APPLICATION_ID) {
            return
        }

        val key = dataIntent.type       // 此次请求的key
        val value = dataIntent.action   // 请求的值

        registeredMessageHandler[key]?.handle(runningInfo, param, value)
    }
}