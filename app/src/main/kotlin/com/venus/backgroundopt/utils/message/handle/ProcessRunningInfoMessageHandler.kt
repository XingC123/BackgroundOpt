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

package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createJsonResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/5/30
 */
class ProcessRunningInfoMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createJsonResponse<Int>(
            param = param,
            value = value
        ) { pid ->
            val processRecord = runningInfo.getRunningProcess(pid) ?: return
            ProcessRunningInfo().apply {
                rssInBytes = processRecord.getCurRssInBytes()
                adj = processRecord.getCurAdjNative()
                originalAdj = processRecord.oomAdjScore
            }
        }
    }

    class ProcessRunningInfo {
        var rssInBytes: Long = Long.MIN_VALUE
        var adj: Int = Int.MIN_VALUE
        var originalAdj: Int = Int.MIN_VALUE

        companion object {
            @JvmStatic
            val singleton by lazy {
                ProcessRunningInfo()
            }
        }
    }
}