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

package com.venus.backgroundopt.hook.handle.android.entity

import android.os.Build
import com.venus.backgroundopt.annotation.AndroidMethod
import com.venus.backgroundopt.annotation.AndroidMethodParam
import com.venus.backgroundopt.annotation.AndroidObject
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.SystemUtils
import com.venus.backgroundopt.utils.callMethod

/**
 * @author XingC
 * @date 2024/4/12
 */
class AppProfiler(
    @AndroidObject(classPath = ClassConstants.AppProfiler)
    override val originalInstance: Any,
) : IAndroidEntity {
    companion object {
        /* *************************************************************************
         *                                                                         *
         * updateLowMemStateLSP                                                    *
         *                                                                         *
         **************************************************************************/
        private val updateLowMemStateLSPMethod = if (SystemUtils.isUOrHigher) {
            { appProfiler: Any, numCached: Int, numEmpty: Int, numTrimming: Int, now: Long ->
                appProfiler.callMethod<Boolean>(
                    methodName = MethodConstants.updateLowMemStateLSP,
                    numCached,
                    numEmpty,
                    numTrimming,
                    now
                )
            }
        } else {
            { appProfiler: Any, numCached: Int, numEmpty: Int, numTrimming: Int, _: Long ->
                appProfiler.callMethod<Boolean>(
                    methodName = MethodConstants.updateLowMemStateLSP,
                    numCached,
                    numEmpty,
                    numTrimming
                )
            }
        }

        @JvmStatic
        @AndroidMethod
        fun updateLowMemStateLSP(
            appProfiler: Any,
            numCached: Int,
            numEmpty: Int,
            numTrimming: Int,
            @AndroidMethodParam(since = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) now: Long,
        ): Boolean = updateLowMemStateLSPMethod(appProfiler, numCached, numEmpty, numTrimming, now)
    }
}