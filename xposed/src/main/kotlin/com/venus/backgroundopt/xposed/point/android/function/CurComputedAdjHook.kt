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

package com.venus.backgroundopt.xposed.point.android.function

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.FunctionHook
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.afterHook
import com.venus.backgroundopt.xposed.util.beforeHook

/**
 * @author XingC
 * @date 2024/10/21
 */
@FunctionHook("计算进程的adj后进行保存")
class CurComputedAdjHook(classLoader: ClassLoader, runningInfo: RunningInfo) :
    IHook(classLoader, runningInfo) {
    override fun hook() {
        val isVOrHigher = OsUtils.isVOrHigher
        // a15
        ClassConstants.OomAdjuster.beforeHook(
            enable = isVOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.setIntermediateAdjLSP,
            hookAllMethod = true
        ) { param ->
            val app = param.args[0]
            val pid = ProcessRecord.getPid(app)
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@beforeHook

            val processStateRecord = processRecord.processStateRecord
            processStateRecord.curComputedAdj = processStateRecord.curRawAdj
        }

        // a11 - a14
        val methodName = if (OsUtils.androidVersionCode >= OsUtils.S
            && OsUtils.androidVersionCode <= OsUtils.U
        ) {
            MethodConstants.computeOomAdjLSP
        } else {
            MethodConstants.computeOomAdjLocked
        }
        ClassConstants.OomAdjuster.afterHook(
            enable = !isVOrHigher,
            classLoader = classLoader,
            methodName = methodName,
            hookAllMethod = true
        ) { param ->
            val app = param.args[0]
            val pid = ProcessRecord.getPid(app)
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@afterHook

            val processStateRecord = processRecord.processStateRecord
            processStateRecord.curComputedAdj = processStateRecord.curRawAdj
        }
    }
}