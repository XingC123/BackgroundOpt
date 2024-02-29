package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.handle.android.entity.PackageManagerService
import com.venus.backgroundopt.utils.afterConstructorHook

/**
 * @author XingC
 * @date 2024/2/29
 */
class PackageManagerServiceHookNew(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.PackageManagerService.afterConstructorHook(
            classLoader = classLoader,
            hookAllMethod = true
        ) { param ->
            runningInfo.packageManagerService = PackageManagerService(param.thisObject)
        }
    }
}