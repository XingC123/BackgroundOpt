package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
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
        // 版本号
        // 用以针对不同版本时期的配置进行不同的逻辑
        var versionCode: Int = -1
        var versionName: String = "none"

        lateinit var packageName: String
        var disableForegroundTrimMem = false
        var disableBackgroundTrimMem = false
        var disableBackgroundGc = true

        // 自定义的主进程oom分数
        var enableCustomMainProcessOomScore = false
        var customMainProcessOomScore = Int.MIN_VALUE
    }
}