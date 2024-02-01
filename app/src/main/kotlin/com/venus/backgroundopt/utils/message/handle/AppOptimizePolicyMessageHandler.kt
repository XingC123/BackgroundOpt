package com.venus.backgroundopt.utils.message.handle

import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * app优化策略消息处理器
 *
 * @author XingC
 * @date 2023/11/5
 */
class AppOptimizePolicyMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<AppOptimizePolicy>(param, value) { appOptimizePolicy ->
            CommonProperties.appOptimizePolicyMap[appOptimizePolicy.packageName] = appOptimizePolicy
            null
        }
    }

    class AppOptimizePolicy : MessageFlag {
        lateinit var packageName: String

        @Deprecated(
            message = "容易误解",
            replaceWith = ReplaceWith(expression = "enableForegroundTrimMem")
        )
        @JSONField(serialize = false)
        var disableForegroundTrimMem: Boolean? = null

        @Deprecated(
            message = "容易误解",
            replaceWith = ReplaceWith(expression = "enableBackgroundTrimMem")
        )
        @JSONField(serialize = false)
        var disableBackgroundTrimMem: Boolean? = null

        @Deprecated(
            message = "容易误解",
            replaceWith = ReplaceWith(expression = "enableBackgroundGc")
        )
        @JSONField(serialize = false)
        var disableBackgroundGc: Boolean? = null

        var enableForegroundTrimMem: Boolean? = null
        var enableBackgroundTrimMem: Boolean? = null
        var enableBackgroundGc: Boolean? = null

        // 自定义的主进程oom分数
        var enableCustomMainProcessOomScore = false
        var customMainProcessOomScore = Int.MIN_VALUE
    }
}