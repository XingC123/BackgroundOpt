package com.venus.backgroundopt.utils.message.handle

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
        var disableForegroundTrimMem = false
        var disableBackgroundTrimMem = false
        var disableBackgroundGc = false
    }
}