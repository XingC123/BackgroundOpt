package com.venus.backgroundopt.utils.message

import com.venus.backgroundopt.entity.RunningInfo
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * 信息处理接口
 *
 * @author XingC
 * @date 2023/9/23
 */
interface MessageHandler {
    fun handle(runningInfo: RunningInfo, param: MethodHookParam, value: String?)
}