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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalMethodParam
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.AppProfilerCompatSinceA14
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.AppProfilerCompatUntilA13
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper

/**
 * @author XingC
 * @date 2024/4/12
 */
abstract class AppProfiler(
    override val originalInstance: Any,
) : IEntityWrapper, IEntityCompatFlag {
    abstract fun updateLowMemStateLSP(
        numCached: Int,
        numEmpty: Int,
        numTrimming: Int,
        @OriginalMethodParam(since = OsUtils.U) now: Long,
    ): Boolean

    object AppProfilerHelper : IEntityCompatHelper<AppProfiler> {
        override val instanceClazz: Class<out AppProfiler>
        override val instanceCreator: (Any) -> AppProfiler

        init {
            if (OsUtils.isUOrHigher) {
                instanceClazz = AppProfilerCompatSinceA14::class.java
                instanceCreator = ::createAppProfilerSinceA14
            } else {
                instanceClazz = AppProfilerCompatUntilA13::class.java
                instanceCreator = ::createAppProfilerUntilA13
            }
        }

        @JvmStatic
        fun createAppProfilerSinceA14(@OriginalObject instance: Any): AppProfiler =
            AppProfilerCompatSinceA14(instance)

        @JvmStatic
        fun createAppProfilerUntilA13(@OriginalObject instance: Any): AppProfiler =
            AppProfilerCompatUntilA13(instance)
    }
}