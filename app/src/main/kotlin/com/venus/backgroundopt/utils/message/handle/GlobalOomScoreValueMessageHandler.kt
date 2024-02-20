package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/2/20
 */
class GlobalOomScoreValueMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Int>(
            param = param,
            value = value
        ) { score ->
            if (ProcessList.NATIVE_ADJ <= score && score < ProcessList.UNKNOWN_ADJ) {
                CommonProperties.globalOomScorePolicy.value.customGlobalOomScore = score
            } else {
                logger.warn("不合法的全局OOM分数: ${score}")
            }
            null
        }
    }
}