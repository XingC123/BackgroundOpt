package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/2/9
 */
class CachedAppOptimizerHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        // 模块接管了压缩行为
        ClassConstants.CachedAppOptimizer.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.onOomAdjustChanged,
            hookAllMethod = true
        ) { it.result = null }
    }
}