package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.core.RunningInfo.AppGroupEnum
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
            /*runningInfo.processManager.compactProcessInfos.onEach { process ->
                // 设置真实oom_adj_score
                process.curAdj = process.getCurAdjNative()
            }*/
            runningInfo.runningAppInfos.asSequence()
                .filterNotNull()
                .filter { it.appGroupEnum == AppGroupEnum.IDLE || it.appGroupEnum == AppGroupEnum.NONE }
                .flatMap { it.processes }
                .onEach {
                    // 设置真实oom_adj_score
                    it.curAdj = it.getCurAdjNative()
                }
                .toList()
        }
    }
}