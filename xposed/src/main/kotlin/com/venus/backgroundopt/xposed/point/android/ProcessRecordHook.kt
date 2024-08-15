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

import com.venus.backgroundopt.common.util.containsIgnoreCase
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.manager.process.oom.isHighPriorityProcess
import com.venus.backgroundopt.xposed.util.beforeHook

/**
 * @author XingC
 * @date 2024/3/14
 */
class ProcessRecordHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    /**
     * @see [ProcessRecordHookForTest]
     */
    val dontKillReasons = arrayOf(
        "anr",
        // 已在 OomAdjusterHookNew 中重写了 updateAndTrimProcessLSP() 的逻辑, 以下情况并不会出现
        // "cached #",/* description: "too many cached" */
        // "empty for",/* description: "empty for too long" */
        // "empty #",/* description: "too many empty" */
        // "swap low and too many cached",
    )

    private fun isReasonInDontKillReasons(reason: String): Boolean {
        dontKillReasons.forEach { predefinedReason ->
            if (reason.containsIgnoreCase(predefinedReason)) {
                return true
            }
        }
        return false
    }

    override fun hook() {
        ClassConstants.ProcessRecord.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.killLocked,
            hookAllMethod = true,
        ) { param ->
            val process = param.thisObject
            val pid = ProcessRecord.getPid(process)
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@beforeHook
            if (!processRecord.isHighPriorityProcess()) {
                return@beforeHook
            }
            val reason = param.args[0] as String
            if (isReasonInDontKillReasons(reason = reason)) {
                param.result = null
//                logger.info("阻止杀死 -> 包名: ${processRecord.packageName}, 原因: ${reason}, ")
            }
        }
    }
}