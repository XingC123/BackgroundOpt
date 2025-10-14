/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat

import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.CachedAppOptimizer
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ICachedAppOptimizer
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.reflect.callMethod
import de.robv.android.xposed.XposedHelpers

/**
 * @author XingC
 * @date 2024/10/28
 */
class CachedAppOptimizerA12(originalInstance: Any) : CachedAppOptimizer(originalInstance) {
    init {
        mCompactThrottleMinOomAdj = XposedHelpers.getLongField(
            originalInstance,
            FieldConstants.mCompactThrottleMinOomAdj
        )
        mCompactThrottleMaxOomAdj = XposedHelpers.getLongField(
            originalInstance,
            FieldConstants.mCompactThrottleMaxOomAdj
        )
    }

    companion object : ICachedAppOptimizer {
        override fun compactProcess(
            @OriginalObject instance: Any,
            pid: Int,
            compactionFlags: Int,
        ): Boolean {
            return synchronized(processCompactLock) {
                runCatchThrowable(defaultValue = false) {
                    instance.callMethod(
                        methodName = MethodConstants.compactProcess,
                        pid,
                        compactionFlags
                    )
                    true
                }!!
            }
        }
    }
}