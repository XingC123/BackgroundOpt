package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
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
            val processes = arrayListOf<ProcessRecordKt>()
            runningInfo.runningAppInfos.forEach { appInfo ->
                appInfo.processes.forEach { process->
                    // 设置真实oom_adj_score
                    process.curAdj = process.getCurAdjNative()
                    processes.add(process)
                }
            }
            processes
        }
    }
}