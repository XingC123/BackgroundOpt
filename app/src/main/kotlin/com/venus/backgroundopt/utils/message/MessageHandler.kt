package com.venus.backgroundopt.utils.message

import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.utils.log.ILogger
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * 信息处理接口
 *
 * @author XingC
 * @date 2023/9/23
 */
interface MessageHandler:ILogger {
    fun handle(runningInfo: RunningInfo, param: MethodHookParam, value: String?)
}