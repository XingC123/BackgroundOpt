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

package com.venus.backgroundopt.xposed.point.android

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.beforeHook

/**
 * @author XingC
 * @date 2024/1/31
 */
class AppProfilerHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    override fun enableHook(): Boolean = OsUtils.isSOrHigher

    override fun hook() {
        // 本模块的内存回收替代
        // 此句在A12及以上存在
        ClassConstants.AppProfiler.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.trimMemoryUiHiddenIfNecessaryLSP,
            hookAllMethod = true
        ) { it.result = null }
    }
}