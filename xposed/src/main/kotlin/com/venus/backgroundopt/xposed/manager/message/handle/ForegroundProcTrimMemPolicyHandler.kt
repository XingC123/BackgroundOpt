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

import com.venus.backgroundopt.common.entity.message.ForegroundProcTrimMemLevelEnum
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * 配置前台进程内存紧张策略
 *
 * @author XingC
 * @date 2023/11/3
 */
object ForegroundProcTrimMemPolicyHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
    ) {
        createResponse<Pair<String, String>>(param, value) { pair ->
            val enumName = pair.second
            try {
                val policyEnum = ForegroundProcTrimMemLevelEnum.valueOf(enumName)
                HookCommonProperties.foregroundProcTrimMemPolicy.value.foregroundProcTrimMemLevelEnum =
                    policyEnum
                logger.info("前台进程内存紧张策略修改为: ${policyEnum.uiName}")
            } catch (t: Throwable) {
                logger.warn("错误的前台内存回收等级", t)
            }
            null
        }
    }
}
