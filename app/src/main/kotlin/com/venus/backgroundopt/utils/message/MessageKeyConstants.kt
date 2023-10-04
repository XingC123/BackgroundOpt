package com.venus.backgroundopt.utils.message

/**
 * ui与模块所在进程通信所使用的消息标识
 *
 * @author XingC
 * @date 2023/9/21
 */
interface MessageKeyConstants {
    companion object {
        const val getRunningAppInfo = "getRunningAppInfo"
        const val getTargetAppGroup = "getTargetAppGroup"
        const val getBackgroundTasks = "getBackgroundTasks"
        const val getAppCompactList = "getAppCompactList"
        const val subProcessOomConfigChange = "subProcessOomConfigChange"
        const val getInstalledApps = "getInstalledApps"
    }
}