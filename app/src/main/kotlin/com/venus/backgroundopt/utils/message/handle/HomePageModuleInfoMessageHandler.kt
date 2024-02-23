package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.message.IMessage
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/2/23
 */
class HomePageModuleInfoMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Any>(
            param = param,
            value = value,
            setJsonData = true
        ) { _ ->
            HomePageModuleInfoMessage().apply {
                defaultMaxAdjStr = ProcessRecordKt.defaultMaxAdjStr
                minOptimizeRssInMBytesStr = ProcessRecordKt.minOptimizeRssInMBytesStr
            }
        }
    }
}

class HomePageModuleInfoMessage : IMessage {
    var defaultMaxAdjStr: String? = null
    var minOptimizeRssInMBytesStr: String? = null
}
