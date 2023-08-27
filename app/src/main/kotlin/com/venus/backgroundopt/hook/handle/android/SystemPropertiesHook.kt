package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.log.ILogger
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/27
 */
class SystemPropertiesHook(
    classLoader: ClassLoader?,
    hookInfo: RunningInfo?
) : MethodHook(classLoader, hookInfo), ILogger {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.SystemProperties,
                MethodConstants.getBoolean,
                arrayOf(
                    beforeHookAction {
                        handleGetBoolean(it)
                    }
                ),
                String::class.java,
                Boolean::class.java
            )
        )
    }

    private fun logPrefix(): String = "SystemProperties: "

    private fun handleGetBoolean(param: MethodHookParam) {
        when (param.args[0] as String) {
            "persist.sys.spc.enabled" -> {
                logger.debug("${logPrefix()}persist.sys.spc.enabled >>> return false")
                param.result = false
            }
        }
    }
}