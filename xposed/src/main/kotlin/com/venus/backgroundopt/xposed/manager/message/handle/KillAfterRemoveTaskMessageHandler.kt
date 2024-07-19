package com.venus.backgroundopt.xposed.manager.message.handle

import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponseWithNullData
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/3/15
 */
class KillAfterRemoveTaskMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
    ) {
        createResponseWithNullData<Boolean>(
            param = param,
            value = value
        ) { isEnabled ->
            HookCommonProperties.enableKillAfterRemoveTask.value = isEnabled
        }
    }
}