package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.runCatchThrowable

/**
 * @author XingC
 * @date 2024/3/18
 */
class WindowProcessControllerHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        // 只要当前hook的方法返回true, 那么就将当前进程加入查杀列表
        ClassConstants.WindowProcessController.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.shouldKillProcessForRemovedTask,
            hookAllMethod = true
        ) { param ->
            val shouldKillProc = param.result as Boolean
            if (!shouldKillProc) {
                return@afterHook
            }

            val windowProcessController = param.thisObject
            val hasForegroundServices = windowProcessController.callMethod(
                methodName = MethodConstants.hasForegroundServices
            ) as Boolean
            if (hasForegroundServices) {
                // 加入待移除列表
                ActivityTaskSupervisorHook.removedTaskWindowProcessControllerSet.add(
                    windowProcessController
                )
            }
        }
    }
}