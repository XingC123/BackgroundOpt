package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 获取app压缩列表的消息处理器
 *
 * @author XingC
 * @date 2023/9/25
 */
class AppCompactListMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Any>(param, value, setJsonData = true) {
            runningInfo.processManager.compactProcessInfos.apply {
                val map = runningInfo.processManager.compactProcessingResultMap
                forEach { process ->
                    // 设置真实oom_adj_score
                    process.curAdj = process.getCurAdjNative()
                    // 设置上一次执行状态
                    process.processingResult = map[process]
                }
            }
        }
    }
}