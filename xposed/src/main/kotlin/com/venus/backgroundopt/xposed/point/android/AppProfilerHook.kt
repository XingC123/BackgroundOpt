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
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.reflect.beforeHook

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
        val isEnableMemTrim = computeIsEnableMemTrimTask()

        // 本模块的内存回收替代
        // 此句在A12及以上存在
        // 若启用了模块的内存回收, 则会禁用系统的内存回收
        logger.info("[${if (isEnableMemTrim) "禁用" else "启用"}] 系统内存回收策略")
        ClassConstants.AppProfiler.beforeHook(
            enable = isEnableMemTrim,
            classLoader = classLoader,
            methodName = MethodConstants.trimMemoryUiHiddenIfNecessaryLSP,
            hookAllMethod = true
        ) { it.result = null }
    }

    /**
     * 计算是否开启了内存回收任务
     */
    private fun computeIsEnableMemTrimTask(): Boolean {
        val isEnabledForegroundProcTrimMem = HookCommonProperties.isEnableForegroundProcTrimMem()
        val isEnabledBackgroundProcTrimMem = HookCommonProperties.isEnableBackgroundProcTrimMem()
        return isEnabledForegroundProcTrimMem or isEnabledBackgroundProcTrimMem
    }
}