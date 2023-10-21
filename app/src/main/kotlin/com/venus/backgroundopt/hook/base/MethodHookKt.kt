package com.venus.backgroundopt.hook.base

import com.venus.backgroundopt.core.RunningInfo

/**
 * @author XingC
 * @date 2023/10/17
 */
open class MethodHookKt(
    classLoader: ClassLoader,
    runningInfo: RunningInfo? = null
) : MethodHook(classLoader, runningInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf()
    }
}