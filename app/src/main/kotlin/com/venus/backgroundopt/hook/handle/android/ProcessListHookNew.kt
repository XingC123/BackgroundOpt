package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/1/31
 */
class ProcessListHookNew(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.ProcessList.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.killAppIfBgRestrictedAndCachedIdleLocked,
            paramTypes = arrayOf(ClassConstants.ProcessRecord, Long::class.javaPrimitiveType)
        ) { it.result = 0L }
    }
}