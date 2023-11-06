package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 启用或禁用前台进程内存紧张
 *
 * @author XingC
 * @date 2023/11/3
 */
class EnableForegroundProcTrimMemPolicyHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Boolean>(param, value) { isEnable ->
            runningInfo.processManager.configureForegroundTrimCheckTask(isEnable)
        }
    }
}