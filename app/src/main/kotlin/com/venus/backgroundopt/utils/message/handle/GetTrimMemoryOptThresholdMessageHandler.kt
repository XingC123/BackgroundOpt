package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/2/22
 */
class GetTrimMemoryOptThresholdMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Any>(
            param = param,
            value = value
        ) { _ ->
            ProcessRecordKt.minOptimizeRssInMBytesStr
        }
    }
}