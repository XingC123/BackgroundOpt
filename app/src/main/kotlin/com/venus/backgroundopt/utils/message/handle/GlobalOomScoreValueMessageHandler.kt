package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
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
            if (GlobalOomScorePolicy.isCustomGlobalOomScoreIllegal(score)) {
                CommonProperties.globalOomScorePolicy.value.customGlobalOomScore = score
                logger.info("全局oom分数切换为: $score")
            } else {
                logger.warn("不合法的全局OOM分数: ${score}")
            }
            null
        }
    }
}