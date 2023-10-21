package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.NULL_DATA
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 获取指定应用的当前内存分组
 *
 * @author XingC
 * @date 2023/9/23
 */
class TargetAppGroupMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Int>(param, value) { uid ->
            runningInfo.getRunningAppInfo(uid)?.appGroupEnum?.name ?: NULL_DATA
        }
    }
}