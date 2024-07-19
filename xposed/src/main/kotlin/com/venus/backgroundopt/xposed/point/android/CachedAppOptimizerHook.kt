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
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.afterHook
import com.venus.backgroundopt.xposed.util.beforeHook
import com.venus.backgroundopt.xposed.util.findClassIfExists
import com.venus.backgroundopt.xposed.util.getLongFieldValue
import com.venus.backgroundopt.xposed.util.replaceHook
import com.venus.backgroundopt.xposed.util.setStaticObjectFieldValue

/**
 * @author XingC
 * @date 2024/2/9
 */
class CachedAppOptimizerHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        // 模块接管了压缩行为
        ClassConstants.CachedAppOptimizer.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.onOomAdjustChanged,
            hookAllMethod = true
        ) { it.result = null }

        // 关闭系统的内存压缩
        runCatchThrowable(catchBlock = { logger.warn("设置系统的默认压缩行为失败", it) }) {
            ClassConstants.CachedAppOptimizer
                .findClassIfExists(classLoader)
                ?.setStaticObjectFieldValue(
                    fieldName = FieldConstants.DEFAULT_USE_COMPACTION,
                    value = false
                )
            logger.info("[禁用]框架层内存压缩")
        }

        ClassConstants.CachedAppOptimizer.replaceHook(
            classLoader = classLoader,
            methodName = MethodConstants.useCompaction,
        ) { false }

        // hook系统freeze进程的延迟时间
        ClassConstants.CachedAppOptimizer.afterHook(
            enable = OsUtils.isSOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.updateFreezerDebounceTimeout,
        ) {
            val cachedAppOptimizerInstance =
                runningInfo.activityManagerService.oomAdjuster.cachedAppOptimizer.originalInstance
            val fieldValue =
                cachedAppOptimizerInstance.getLongFieldValue(FieldConstants.mFreezerDebounceTimeout)
            runningInfo.activityManagerService.oomAdjuster.cachedAppOptimizer.mFreezerDebounceTimeout =
                fieldValue

            logger.info("系统freeze进程的延迟时间: ${fieldValue}")
        }
    }
}