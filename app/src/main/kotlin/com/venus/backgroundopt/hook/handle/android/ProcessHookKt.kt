package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.Process
import com.venus.backgroundopt.manager.process.ProcessManager
import com.venus.backgroundopt.utils.concurrent.lock
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/19
 */
class ProcessHookKt(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    companion object {
        // 在此处的App内存状态将不会允许系统设置ProcessGroup
        val ignoreSetProcessGroupAppGroups = arrayOf(
            AppGroupEnum.IDLE,
            AppGroupEnum.TMP,
            AppGroupEnum.ACTIVE
        )
    }

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
            /*HookPoint(
                ClassConstants.Process,
                MethodConstants.setProcessGroup,
                arrayOf(
                    beforeHookAction { handleSetProcessGroup(it) }
                ),
                Int::class.javaPrimitiveType,   // pid
                Int::class.javaPrimitiveType    // group
            ),*/
        )
    }

    private fun handleKillApp(param: MethodHookParam) {
        val uid = param.args[0] as Int
        val appInfo = runningInfo.getRunningAppInfo(uid) ?: return

        val pid = param.args[1] as Int
        val processRecordKt = appInfo.getProcess(pid)
        val mainProcess = processRecordKt?.mainProcess ?: false
        val packageName = appInfo.packageName

        if (mainProcess) {
            runningInfo.removeRunningApp(appInfo)
            if (BuildConfig.DEBUG) {
                logger.debug("kill: ${packageName}, uid: $uid >>> 杀死App")
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

    private fun handleSetProcessGroup(param: MethodHookParam) {
        val pid = param.args[0] as Int
        val group = param.args[1] as Int

        if (group > Process.THREAD_GROUP_RESTRICTED) {  //若是模块控制的行为, 则直接处理
            //若是模块控制的行为, 则直接处理
            param.args[1] = group - ProcessManager.THREAD_GROUP_LEVEL_OFFSET
            if (BuildConfig.DEBUG) {
                logger.debug("pid: ${pid}设置ProcessGroup >>> ${param.args[1]}")
            }
        } else {
            val uid = Process.getUidForPid(pid)
            val appInfo = runningInfo.getRunningAppInfo(uid)

            // 模块接管此处行为
            if (appInfo != null && appInfo.appGroupEnum in ignoreSetProcessGroupAppGroups) {
                param.result = null
            }
        }
    }
}