package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.hook.HookCommonProperties
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponseWithNullData
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/3/15
 */
class KillAfterRemoveTaskMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponseWithNullData<Boolean>(
            param = param,
            value = value
        ) { isEnabled ->
            HookCommonProperties.enableKillAfterRemoveTask.value = isEnabled
        }
    }
}