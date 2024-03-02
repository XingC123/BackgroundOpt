package com.venus.backgroundopt.hook.handle.android

import android.os.IBinder
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.constants.ServiceConstants
import com.venus.backgroundopt.utils.afterHook

/**
 * @author XingC
 * @date 2024/2/29
 */
class ServiceManagerHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.ServiceManager.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.addService,
            hookAllMethod = true
        ) { param ->
            val name = param.args[0] as String
            val service = param.args[1] as IBinder

            when(name) {
                ServiceConstants.role -> runningInfo.initActiveLaunchPackageName()
            }
        }
    }
}