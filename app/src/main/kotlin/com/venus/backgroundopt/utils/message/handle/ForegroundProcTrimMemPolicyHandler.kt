package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 配置前台进程内存紧张策略
 *
 * @author XingC
 * @date 2023/11/3
 */
class ForegroundProcTrimMemPolicyHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Pair<String, String>>(param, value) { pair ->
            CommonProperties.foregroundProcTrimMemPolicyMap[pair.second]?.let {
                runningInfo.processManager.setForegroundTrimLevel(it)

                logger.info("前台进程内存紧张策略修改为: ${pair.first}")
            }
        }
    }
}