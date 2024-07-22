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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat

import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.AppProfiler
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.IAppProfiler
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod

/**
 * @author XingC
 * @date 2024/7/13
 */
class AppProfilerCompatSinceA14(
    originalInstance: Any,
) : AppProfiler(originalInstance) {
    override fun updateLowMemStateLSP(
        numCached: Int,
        numEmpty: Int,
        numTrimming: Int,
        now: Long,
    ): Boolean {
        return originalInstance.callMethod<Boolean>(
            methodName = MethodConstants.updateLowMemStateLSP,
            numCached,
            numEmpty,
            numTrimming,
            now
        )
    }

    companion object : IAppProfiler
}