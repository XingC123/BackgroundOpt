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

package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.beforeHook

/**
 * @author XingC
 * @date 2024/3/14
 */
class ProcessRecordHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    /**
     * @see [ProcessRecordHookForTest]
     */
    val dontKillReasons = arrayOf(
        "anr",
        "cached #",/* description: "too many cached" */
        "empty for",/* description: "empty for too long" */
        "empty #",/* description: "too many empty" */
        "swap low and too many cached",
    )

    private fun isReasonInDontKillReasons(reason: String): Boolean {
        dontKillReasons.forEach { predefinedReason ->
            if (reason.contains(predefinedReason)) {
                return true
            }
        }
        return false
    }

    override fun hook() {
        ClassConstants.ProcessRecord.beforeHook(
            classLoader = classLoader,
            methodName = "killLocked",
            hookAllMethod = true,
        ) { param ->
            val process = param.thisObject
            val pid = ProcessRecordKt.getPid(process)
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@beforeHook
            if (!ProcessListHookKt.isHighLevelProcess(processRecord)) {
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