package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/19
 */
class ProcessHookKt(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.Process,
                MethodConstants.killProcessGroup,
                arrayOf(
                    beforeHookAction {
                        handleKillApp(it)
                    }
                ),
                Int::class.java,
                Int::class.java
            ),
        )
    }

    private fun handleKillApp(param: MethodHookParam) {
        val uid = param.args[0] as Int
        val pid = param.args[1] as Int
        val appInfo = runningInfo.getRunningAppInfo(uid)

        appInfo?.let {
            var mPid = Int.MIN_VALUE
            try {
                mPid = appInfo.getmPid()
            } catch (ignore: Exception) {
            }
            if (mPid == Int.MIN_VALUE) {
                if (BuildConfig.DEBUG) {
                    logger.warn("kill: ${appInfo.packageName}, uid: $uid >>>  mpid获取失败")
                }
            } else if (pid != mPid) {   // 处理子进程
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
}