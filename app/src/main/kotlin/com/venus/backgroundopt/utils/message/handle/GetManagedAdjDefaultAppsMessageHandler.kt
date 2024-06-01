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
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createJsonResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/4/23
 */
class GetManagedAdjDefaultAppsMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createJsonResponse<Any?>(
            param = param,
            value = value,
        ) { _ ->
            ManagedAdjDefaultAppMessage().apply {
                defaultAppPackageNames = runningInfo.allDefaultPackageNames
            }
        }
    }

    class ManagedAdjDefaultAppMessage : MessageFlag {
        var defaultAppPackageNames: Collection<String>? = null
    }
}