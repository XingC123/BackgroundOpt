package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/2/1
 */
class OomAdjusterHookNew(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.OomAdjuster.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateAndTrimProcessLSP,
            hookAllMethod = true,
        ) { it.args[2] = 0 }
    }
}