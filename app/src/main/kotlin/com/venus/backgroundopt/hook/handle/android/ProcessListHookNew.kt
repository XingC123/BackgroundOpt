package com.venus.backgroundopt.hook.handle.android

import android.os.Build
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
            enable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            classLoader = classLoader,
            methodName = MethodConstants.killAppIfBgRestrictedAndCachedIdleLocked,
            paramTypes = arrayOf(ClassConstants.ProcessRecord, Long::class.javaPrimitiveType)
        ) { it.result = 0L }
    }
}