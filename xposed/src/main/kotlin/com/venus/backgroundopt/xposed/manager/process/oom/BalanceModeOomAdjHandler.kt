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

/**
 * [OomWorkModePref.MODE_BALANCE]
 *
 * @author XingC
 * @date 2024/7/17
 */
open class BalanceModeOomAdjHandler(
    userProcessMinAdj: Int = BACKGROUND_ADJ_START,
    userProcessMaxAdj: Int = ProcessRecord.defaultMaxAdj,
    adjConvertFactor: Int = ADJ_CONVERT_FACTOR,
    highPrioritySubprocessAdjOffset: Int = HIGH_PRIORITY_SUBPROCESS_ADJ_OFFSET,
) : OomAdjHandler(
    userProcessMinAdj,
    userProcessMaxAdj,
    adjConvertFactor,
    highPrioritySubprocessAdjOffset
) {
    private val highPrioritySubprocessAdj = userProcessMinAdj + highPrioritySubprocessAdjOffset

    override fun checkAndSetDefaultMaxAdjIfNeed(processRecord: ProcessRecord) {
        // do nothing
    }

    override fun computeMainProcessAdj(adj: Int): Int {
        return userProcessMinAdj
    }

    override fun computeHighPrioritySubprocessAdj(adj: Int): Int {
        return highPrioritySubprocessAdj
    }
}