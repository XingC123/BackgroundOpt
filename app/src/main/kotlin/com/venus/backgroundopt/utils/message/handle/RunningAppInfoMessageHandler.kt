package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 处理"获取已记录的AppInfo"
 *
 * @author XingC
 * @date 2023/9/23
 */
class RunningAppInfoMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse(param, value) { uid: Int ->
            runningInfo.getRunningAppInfo(uid)
        }
    }
}