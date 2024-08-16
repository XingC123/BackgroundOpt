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

package com.venus.backgroundopt.xposed.manager.message.handle

import com.venus.backgroundopt.common.entity.message.ModuleRunningMessage
import com.venus.backgroundopt.common.entity.message.ModuleRunningMessage.Companion.MODULE_RUNNING
import com.venus.backgroundopt.common.entity.message.ModuleRunningMessage.Companion.MODULE_VERSION_CODE
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/4/19
 */
object ModuleRunningMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
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
}