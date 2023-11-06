package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 返回后台任务列表
 *
 * @author XingC
 * @date 2023/9/23
 */
class BackgroundTasksMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Any>(param, value, setJsonData = true) { _ ->
            val processRecordKts = runningInfo.processManager.backgroundTasks.apply {
                ProcessRecordKt.setActualAdj(this)
            }
            BackgroundTaskMessage().apply {
                processInfos = processRecordKts.toMutableList()
                appOptimizePolicyMap = CommonProperties.appOptimizePolicyMap
            }
        }
    }

    class BackgroundTaskMessage : MessageFlag {
        lateinit var processInfos: MutableList<BaseProcessInfoKt>
        lateinit var appOptimizePolicyMap: MutableMap<String, AppOptimizePolicyMessageHandler.AppOptimizePolicy>
    }
}