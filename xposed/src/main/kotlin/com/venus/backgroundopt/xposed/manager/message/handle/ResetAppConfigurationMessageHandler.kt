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

import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.entity.message.ResetAppConfigurationMessage
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2024/7/23
 */
object ResetAppConfigurationMessageHandler : MessageHandler {
    override fun handle(runningInfo: RunningInfo, param: MethodHookParam, value: String?) {
        createResponse<AppItem>(
            param = param,
            value = value,
            setJsonData = true
        ) { appItem ->
            val packageName = appItem.packageName

            // 删除应用配置
            HookCommonProperties.appOptimizePolicyMap.remove(packageName)
            // 删除进程配置
            appItem.processes.forEach { processName ->
                HookCommonProperties.subProcessOomPolicyMap.remove(processName)
            }

            ResetAppConfigurationMessage().apply {
                code = ResetAppConfigurationMessage.RESET_SUCCESS
            }
        }
    }
}