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

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import com.venus.backgroundopt.utils.message.handle.ModuleRunningMessageHandler.ModuleRunningMessage.Companion.MODULE_RUNNING
import com.venus.backgroundopt.utils.message.handle.ModuleRunningMessageHandler.ModuleRunningMessage.Companion.MODULE_VERSION_CODE
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/4/19
 */
class ModuleRunningMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<ModuleRunningMessage>(
            param = param,
            value = value,
            setJsonData = true
        ) { request ->
            val message = ModuleRunningMessage()
            when (request.messageType) {
                MODULE_RUNNING -> {
                    message.value = true
                }

                MODULE_VERSION_CODE -> {
                    message.value = BuildConfig.VERSION_CODE
                }

                else -> {}
            }
            message
        }
    }

    class ModuleRunningMessage : MessageFlag {
        var value: Any? = null
        var messageType: Int = 0

        companion object {
            const val MODULE_RUNNING = 1
            const val MODULE_VERSION_CODE = 2
        }
    }
}