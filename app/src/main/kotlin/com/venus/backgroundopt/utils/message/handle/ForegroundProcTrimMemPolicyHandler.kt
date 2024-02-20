package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 配置前台进程内存紧张策略
 *
 * @author XingC
 * @date 2023/11/3
 */
class ForegroundProcTrimMemPolicyHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Pair<String, String>>(param, value) { pair ->
            val enumName = pair.second
            try {
                val policyEnum = ForegroundProcTrimMemLevelEnum.valueOf(enumName)
                CommonProperties.foregroundProcTrimMemPolicy.value.foregroundProcTrimMemLevelEnum =
                    policyEnum
                logger.info("前台进程内存紧张策略修改为: ${policyEnum.uiName}")
            } catch (t: Throwable) {
                logger.warn("错误的前台内存回收等级", t)
            }
            null
        }
    }
}

class ForegroundProcTrimMemPolicy : MessageFlag {
    var isEnabled = PreferenceDefaultValue.enableForegroundTrimMem
    var foregroundProcTrimMemLevelEnum = ForegroundProcTrimMemLevelEnum.RUNNING_MODERATE
}

enum class ForegroundProcTrimMemLevelEnum(val uiName: String, val level: Int) : MessageFlag {
    RUNNING_MODERATE("系统内存稍低", ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE),
    RUNNING_LOW("系统内存相当低", ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW),
    RUNNING_CRITICAL("系统内存不足", ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL),
    UI_HIDDEN("UI不可见", ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN),
}
