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

import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * app优化策略消息处理器
 *
 * @author XingC
 * @date 2023/11/5
 */
class AppOptimizePolicyMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<AppOptimizePolicy>(param, value) { appOptimizePolicy ->
            CommonProperties.appOptimizePolicyMap[appOptimizePolicy.packageName] = appOptimizePolicy
            null
        }
    }

    class AppOptimizePolicy : MessageFlag {
        lateinit var packageName: String

        @Deprecated(
            message = "容易误解",
            replaceWith = ReplaceWith(expression = "enableForegroundTrimMem")
        )
        @JSONField(serialize = false)
        var disableForegroundTrimMem: Boolean? = null
            set(value) {
                enableForegroundTrimMem = value?.let { !it }
                    ?: PreferenceDefaultValue.enableForegroundTrimMem
                field = value
            }

        @Deprecated(
            message = "容易误解",
            replaceWith = ReplaceWith(expression = "enableBackgroundTrimMem")
        )
        @JSONField(serialize = false)
        var disableBackgroundTrimMem: Boolean? = null
            set(value) {
                enableBackgroundTrimMem = value?.let { !it }
                    ?: PreferenceDefaultValue.enableBackgroundTrimMem
                field = value
            }

        @Deprecated(
            message = "容易误解",
            replaceWith = ReplaceWith(expression = "enableBackgroundGc")
        )
        @JSONField(serialize = false)
        var disableBackgroundGc: Boolean? = null
            set(value) {
                enableBackgroundGc = value?.let { !it } ?: PreferenceDefaultValue.enableBackgroundGc
                field = value
            }

        var enableForegroundTrimMem: Boolean? = null
        var enableBackgroundTrimMem: Boolean? = null
        var enableBackgroundGc: Boolean? = null

        // 自定义的主进程oom分数
        var enableCustomMainProcessOomScore = false
        var customMainProcessOomScore = Int.MIN_VALUE

        // 该app是否管理adj
        // 2024.3.26: 目前仅针对系统app
        var shouldHandleAdj: Boolean? = null
    }
}

/**
 * 获取自定义的主进程oom分数
 * @receiver AppOptimizePolicyMessageHandler.AppOptimizePolicy? app优化策略
 * @return Int? 自定义主进程oom分数
 */
fun AppOptimizePolicyMessageHandler.AppOptimizePolicy?.getCustomMainProcessOomScore(): Int? {
    return if (this?.enableCustomMainProcessOomScore == true &&
        /* 对自定义的主进程adj进行合法性确认 */
        this.customMainProcessOomScore >= ProcessList.NATIVE_ADJ &&
        this.customMainProcessOomScore < ProcessList.UNKNOWN_ADJ
    ) {
        this.customMainProcessOomScore
    } else {
        null
    }
}