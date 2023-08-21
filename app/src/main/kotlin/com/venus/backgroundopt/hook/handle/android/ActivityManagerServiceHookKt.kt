package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.afterHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/19
 */
class ActivityManagerServiceHookKt(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.ActivityManagerService,
                MethodConstants.forceStopPackage,
                arrayOf(
                    afterHookAction {
                        handleForceStopPackage(it)
                    }
                ),
                String::class.java, /* packageName */
                Int::class.java /* userId */
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
}