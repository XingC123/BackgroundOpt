package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 子进程OOM配置修改的消息处理器
 *
 * @author XingC
 * @date 2023/9/28
 */
class SubProcessOomConfigChangeMessageHandler : MessageHandler {
    companion object {
        class SubProcessOomConfigChangeMessage {
            lateinit var processName: String
            lateinit var subProcessOomPolicy: SubProcessOomPolicy
        }
    }

    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<SubProcessOomConfigChangeMessage>(
            param,
            value
        ) { subProcessOomConfigChangeMessage ->
            // 移除或添加oom策略。在下次调整进程oom_adj_score时生效
            if (subProcessOomConfigChangeMessage.subProcessOomPolicy.policyEnum != SubProcessOomPolicy.SubProcessOomPolicyEnum.DEFAULT) {
                CommonProperties.subProcessOomPolicyMap?.put(
                    subProcessOomConfigChangeMessage.processName,
                    subProcessOomConfigChangeMessage.subProcessOomPolicy
                )
            } else {
                CommonProperties.subProcessOomPolicyMap?.remove(subProcessOomConfigChangeMessage.processName)
            }

            if (BuildConfig.DEBUG) {
                logger.debug(
                    "更新子进程oom策略, processName: ${subProcessOomConfigChangeMessage.processName}, " +
                            "策略: ${subProcessOomConfigChangeMessage.subProcessOomPolicy.policyEnum}"
                )
            }
            null
        }
    }
}