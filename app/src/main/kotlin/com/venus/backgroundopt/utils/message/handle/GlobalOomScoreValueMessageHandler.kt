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
import com.venus.backgroundopt.environment.hook.HookCommonProperties
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/2/20
 */
class GlobalOomScoreValueMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Int>(
            param = param,
            value = value
        ) { score ->
            if (GlobalOomScorePolicy.isCustomGlobalOomScoreIllegal(score)) {
                val old = HookCommonProperties.globalOomScorePolicy.value
                HookCommonProperties.globalOomScorePolicy.value = old.apply {
                    customGlobalOomScore = score
                }
                logger.info("全局oom分数切换为: $score")
            } else {
                logger.warn("不合法的全局OOM分数: ${score}")
            }
            null
        }
    }
}