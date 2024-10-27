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

import com.venus.backgroundopt.common.entity.message.SubProcessOomConfigChangeMessage
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 子进程OOM配置修改的消息处理器
 *
 * @author XingC
 * @date 2023/9/28
 */
object SubProcessOomConfigChangeMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
    ) {
        createResponse<SubProcessOomConfigChangeMessage>(
            param,
            value
        ) { subProcessOomConfigChangeMessage ->
            val userId = subProcessOomConfigChangeMessage.userId
            val processKey = subProcessOomConfigChangeMessage.processKey
            val processName = subProcessOomConfigChangeMessage.processName
            val packageName = subProcessOomConfigChangeMessage.packageName
            // 移除或添加oom策略。在下次调整进程oom_adj_score时生效
            if (subProcessOomConfigChangeMessage.subProcessOomPolicy.policyEnum != SubProcessOomPolicy.SubProcessOomPolicyEnum.DEFAULT) {
                HookCommonProperties.replaceSubProcessOomPolicy(
                    subProcessOomConfigChangeMessage.subProcessOomPolicy
                )
            } else {
                HookCommonProperties.removeSubProcessOomPolicy(processKey)
            }

            // 是否需要更改进程的adj处理策略
            ProcessRecord.resetAdjHandleType(
                userId = userId,
                packageName = packageName,
                processName = processName
            )

            if (BuildConfig.DEBUG) {
                logger.debug(
                    "更新子进程oom策略, processName: ${subProcessOomConfigChangeMessage.processName}, " +
                            "策略: ${subProcessOomConfigChangeMessage.subProcessOomPolicy.policyEnum}"
                )
            }
            null
        }
    }
}