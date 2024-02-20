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
class GlobalOomScoreEffectiveScopeMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Pair<String, String>>(
            param = param,
            value = value
        ) { pair ->
            try {
                val enumName = pair.second
                val scopeEnum = GlobalOomScoreEffectiveScopeEnum.valueOf(enumName)
                CommonProperties.globalOomScorePolicy.value.globalOomScoreEffectiveScope = scopeEnum
                logger.info("切换全局oom作用域为: ${scopeEnum.uiName}")
            } catch (t: Throwable) {
                logger.warn("错误的全局oom作用域类型", t)
            }
            null
        }
    }
}