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
class AppProfilerHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        // 根据内存压力来清理进程
        // 新系统使用psi, 旧系统通过检测当前cached和empty的进程个数来确认
        // 最终触发trim memory。本模块的内存回收与其调用方法一致。因此屏蔽掉系统的实现
        // https://www.bluepuni.com/archives/aosp-process-management/
        ClassConstants.AppProfiler.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateLowMemStateLSP,
            hookAllMethod = true,
        ) { it.result = null }
    }
}