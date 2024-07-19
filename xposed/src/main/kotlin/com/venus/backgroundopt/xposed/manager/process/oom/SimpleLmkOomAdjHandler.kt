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

package com.venus.backgroundopt.xposed.manager.process.oom

import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessList
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.self.AppInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import java.util.concurrent.ConcurrentHashMap

/**
 * [HookCommonProperties.useSimpleLmk]
 *
 * @author XingC
 * @date 2024/7/17
 */
class SimpleLmkOomAdjHandler : BalancePlusModeOomAdjHandler() {
    private val systemMainProcessMinAdj: Int = BACKGROUND_ADJ_START
    private val systemMainProcessMaxAdj: Int = 3
    private val systemMainProcessNormalAdj: Int = "%.0f".format(
        (systemMainProcessMinAdj + systemMainProcessMaxAdj) / 2.0
    ).toInt()

    private val systemProcessAdjMap = ConcurrentHashMap<Int, Int>(4)

    init {
        userProcessMinAdj = systemMainProcessMaxAdj + 1
    }

    private fun computeImportAppAdj(adj: Int): Int {
        return systemProcessAdjMap.computeIfAbsent(adj) { _ ->
            if (adj < ProcessList.VISIBLE_APP_ADJ) {
                systemMainProcessMinAdj
            } else if (adj < ProcessList.CACHED_APP_MIN_ADJ) {
                systemMainProcessNormalAdj
            } else {
                systemMainProcessMaxAdj
            }
        }
    }

    override fun computeHighPriorityProcessPossibleAdj(
        processRecord: ProcessRecord,
        adj: Int,
        appInfo: AppInfo,
    ): Int {
        return if (processRecord.mainProcess) {
            if (appInfo.importantSystemApp) {
                computeImportAppAdj(adj)
            } else {
                computeMainProcessAdj(adj)
            }
        } else {
            computeHighPrioritySubprocessAdj(adj)
        }
    }
}