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

package com.venus.backgroundopt.xposed.point.android.function

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.FunctionHook
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.afterConstructorHook
import com.venus.backgroundopt.xposed.util.beforeHook
import com.venus.backgroundopt.xposed.util.replaceHook
import com.venus.backgroundopt.xposed.util.setIntFieldValue

/**
 * @author XingC
 * @date 2024/7/18
 */
@FunctionHook(description = "内存压力传递相关的hook")
class MemoryPressureHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        updateLowMemStateHook()
        memFactorHook()
    }

    /**
     * 根据内存压力来清理进程
     *
     * 新系统使用psi, 旧系统通过检测当前cached和empty的进程个数来确认
     *
     * 最终触发trim memory。本模块的内存回收与其调用方法一致。因此屏蔽掉系统的实现
     *
     * [点击了解](https://www.bluepuni.com/archives/aosp-process-management/)
     */
    private fun updateLowMemStateHook() {
        ClassConstants.AppProfiler.replaceHook(
            enable = OsUtils.isSOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.updateLowMemStateLSP,
            hookAllMethod = true,
        ) { false }

        ClassConstants.ActivityManagerService.replaceHook(
            enable = OsUtils.isR,
            classLoader = classLoader,
            methodName = MethodConstants.updateLowMemStateLocked,
            hookAllMethod = true
        ) { false }
    }

    private fun memFactorHook() {
        // com.android.internal.app.procstats.ProcessStats.ADJ_MEM_FACTOR_NORMAL
        val ADJ_MEM_FACTOR_NORMAL = 0

        /*
         * 安卓12及以上
         */
        ClassConstants.AppProfiler.afterConstructorHook(
            enable = OsUtils.isSOrHigher,
            classLoader = classLoader,
            hookAllMethod = true
        ) {
            it.thisObject.setIntFieldValue(FieldConstants.mMemFactorOverride, ADJ_MEM_FACTOR_NORMAL)
        }

        ClassConstants.AppProfiler.replaceHook(
            enable = OsUtils.isSOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.isLastMemoryLevelNormal,
        ) { true }

        ClassConstants.AppProfiler.replaceHook(
            enable = OsUtils.isSOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.getLastMemoryLevelLocked,
        ) { ADJ_MEM_FACTOR_NORMAL }

        ClassConstants.ActivityManagerService.beforeHook(
            enable = OsUtils.isSOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.setMemFactorOverride,
            paramTypes = arrayOf(Int::class.java)
        ) { it.result = null }
    }
}