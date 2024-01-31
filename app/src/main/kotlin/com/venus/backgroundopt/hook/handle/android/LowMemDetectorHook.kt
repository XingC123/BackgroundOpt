package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/1/31
 */
class LowMemDetectorHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        // 禁止psi检查
        ClassConstants.LowMemThread.beforeHook(
            classLoader = classLoader,
            methodName = "run"
        ) { param ->
            param.result = null
        }
    }
}