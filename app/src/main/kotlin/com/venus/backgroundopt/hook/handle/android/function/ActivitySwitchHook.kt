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

package com.venus.backgroundopt.hook.handle.android.function

import android.app.usage.UsageEvents
import com.venus.backgroundopt.annotation.FunctionHook
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/7/11
 */
@FunctionHook("Hook以感知Activity的切换")
class ActivitySwitchHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?,
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        val runningInfo = runningInfo

        ClassConstants.ActivityTaskManagerService.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateActivityUsageStats,
            hookAllMethod = true
        ) { param ->
            // ActivityRecord
            val activity = param.args[0]

            when (val event = param.args[1] as Int) {
                ACTIVITY_RESUMED, ACTIVITY_STOPPED, ACTIVITY_DESTROYED -> {
                    runningInfo.handleActivityEventChange(event, activity)
                }
            }
        }
    }

    companion object {
        const val ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED
        const val ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED
        const val ACTIVITY_STOPPED = UsageEvents.Event.ACTIVITY_STOPPED

        const val ACTIVITY_DESTROYED = 24
    }
}