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

import com.venus.backgroundopt.common.entity.message.BackgroundTaskMessage
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 返回后台任务列表
 *
 * @author XingC
 * @date 2023/9/23
 */
class BackgroundTasksMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
    ) {
        createResponse<Any>(param, value, setJsonData = true) { _ ->
            val processRecords = runningInfo.processManager.backgroundTasks.onEach {
                // 设置真实oom_adj_score
                it.curAdj = it.getCurAdjNative()
            }
            BackgroundTaskMessage().apply {
                processInfos = processRecords.toMutableList()
                appOptimizePolicyMap = HookCommonProperties.appOptimizePolicyMap
            }
        }
    }
}