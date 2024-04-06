/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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