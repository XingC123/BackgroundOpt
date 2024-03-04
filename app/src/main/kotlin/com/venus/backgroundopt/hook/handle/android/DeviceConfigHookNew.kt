package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.DeviceConfig
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/3/4
 */
class DeviceConfigHookNew(classLoader: ClassLoader?, runningInfo: RunningInfo?) :
    IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.DeviceConfig.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.getBoolean,
            paramTypes = arrayOf(
                String::class.java,
                String::class.java,
                Boolean::class.java,
            )
        ) { param ->
            val namespace = param.args[0] as String
            val key = param.args[1] as String
            if (namespace == DeviceConfig.NAMESPACE_ACTIVITY_MANAGER) {
                if (key == DeviceConfig.KEY_USE_COMPACTION) {
                    param.result = false
                }
            }
        }
    }
}