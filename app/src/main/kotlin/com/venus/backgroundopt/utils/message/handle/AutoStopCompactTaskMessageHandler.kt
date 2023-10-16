package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2023/10/16
 */
class AutoStopCompactTaskMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Boolean>(
            param, value
        ) {
            runningInfo.processManager.setAutoStopCompactTask(it)
        }
    }
}