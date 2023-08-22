package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.afterHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
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
        val pid = param.args[1] as Int

        val uid = ProcessRecord.getUID(processRecord)
        val appInfo = runningInfo.getRunningAppInfo(uid)

        appInfo ?: return

        var mPid = Int.MIN_VALUE
        var flag = false

        try {
            mPid = appInfo.getmPid()
        } catch (e: Exception) {
            // app已清理过一次后台
            flag = true
        }

        if (mPid == pid || flag) {
            runningInfo.removeRunningApp(appInfo)

            if (BuildConfig.DEBUG) {
                logger.debug("kill: ${appInfo.packageName}, uid: $uid >>> 杀死App")
            }
        } else if (mPid == Int.MIN_VALUE) {
            if (BuildConfig.DEBUG) {
                logger.warn("kill: ${appInfo.packageName}, uid: $uid >>> 再次杀死App")
            }
        } else {
            // 移除进程记录
            val processInfo = appInfo.removeProcessInfo(pid)
            // 取消进程的待压缩任务
            runningInfo.processManager.cancelCompactProcessInfo(processInfo)
            if (BuildConfig.DEBUG) {
                logger.debug("kill: ${appInfo.packageName}, uid: ${uid}, pid: $pid >>> 子进程被杀")
            }
        }
    }
}