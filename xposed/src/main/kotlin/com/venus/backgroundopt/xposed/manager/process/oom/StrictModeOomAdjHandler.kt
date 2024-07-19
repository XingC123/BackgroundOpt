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

import com.venus.backgroundopt.common.entity.preference.OomWorkModePref
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.self.AppInfo

/**
 * [OomWorkModePref.MODE_STRICT]
 *
 * @author XingC
 * @date 2024/7/17
 */
open class StrictModeOomAdjHandler(
    userProcessMinAdj: Int = 0,
    userProcessMaxAdj: Int = 0,
    adjConvertFactor: Int = 1,
    highPrioritySubprocessAdjOffset: Int = 0,
) : OomAdjHandler(
    userProcessMinAdj,
    userProcessMaxAdj,
    adjConvertFactor,
    highPrioritySubprocessAdjOffset
) {
    override fun computeHighPriorityProcessPossibleAdj(
        processRecord: ProcessRecord,
        adj: Int,
        appInfo: AppInfo,
    ): Int {
        return 0
    }
}