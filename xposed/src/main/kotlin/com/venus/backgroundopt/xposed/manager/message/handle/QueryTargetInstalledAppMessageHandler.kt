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

package com.venus.backgroundopt.xposed.manager.message.handle

import com.venus.backgroundopt.common.entity.message.QueryInstalledAppParam
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createJsonResponse
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * 根据给定的值, 查询匹配的app
 *
 * @author XingC
 * @date 2024/10/26
 */
object QueryTargetInstalledAppMessageHandler : MessageHandler {
    override fun handle(runningInfo: RunningInfo, param: MethodHookParam, value: String?) {
        createJsonResponse<QueryInstalledAppParam>(
            param = param,
            value = value
        ) { queryParam: QueryInstalledAppParam ->
            runningInfo.packageManagerService?.getPackageInfoAsUser(
                packageName = queryParam.packageName,
                userId = queryParam.userId,
                packageInfoFlag = queryParam.packageInfoFlag
            )
        }
    }
}