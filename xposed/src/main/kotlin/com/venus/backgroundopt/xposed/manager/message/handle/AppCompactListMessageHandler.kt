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

import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 获取app压缩列表的消息处理器
 *
 * @author XingC
 * @date 2023/9/25
 */
class AppCompactListMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
    ) {
        createResponse<Any>(param, value, setJsonData = true) {
            /*runningInfo.processManager.compactProcessInfos.onEach { process ->
                // 设置真实oom_adj_score
                process.curAdj = process.getCurAdjNative()
            }*/
            runningInfo.runningProcessList.asSequence()
                .filter { processRecord ->
                    val appInfo = processRecord.appInfo
                    appInfo.appGroupEnum == AppGroupEnum.IDLE || appInfo.appGroupEnum == AppGroupEnum.NONE
                }
                .onEach {
                    // 设置真实oom_adj_score
                    it.curAdj = it.getCurAdjNative()
                }
                .toList()
        }
    }
}