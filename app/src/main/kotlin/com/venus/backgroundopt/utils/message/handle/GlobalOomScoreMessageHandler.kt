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
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/2/20
 */
class GlobalOomScoreMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<GlobalOomScorePolicy>(
            param = param,
            value = value
        ) { globalOomScorePolicy: GlobalOomScorePolicy ->
            CommonProperties.globalOomScorePolicy.value = globalOomScorePolicy
            logger.info("切换全局oom策略。$globalOomScorePolicy")
            null
        }
    }
}

class GlobalOomScorePolicy : MessageFlag {
    var enabled: Boolean = false
    var globalOomScoreEffectiveScope: GlobalOomScoreEffectiveScopeEnum =
        GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS
    var customGlobalOomScore: Int = Int.MIN_VALUE

    override fun toString(): String {
        return if (enabled) {
            "[启用]全局OOM, 范围: ${globalOomScoreEffectiveScope.uiName}, oom分数: $customGlobalOomScore"
        } else {
            "[禁用]全局OOM"
        }
    }

    companion object {
        @JvmStatic
        fun isCustomGlobalOomScoreIllegal(score: Int): Boolean {
            return ProcessList.NATIVE_ADJ <= score && score < ProcessList.UNKNOWN_ADJ
        }

        @JvmStatic
        fun getCustomGlobalOomScoreIfIllegal(score: Int, defaultValue: Int): Int {
            return if (isCustomGlobalOomScoreIllegal(score)) {
                score
            } else {
                defaultValue
            }
        }
    }
}

enum class GlobalOomScoreEffectiveScopeEnum(val uiName:String) : MessageFlag {
    MAIN_PROCESS("主进程(有界面)"),
    MAIN_AND_SUB_PROCESS("主+子进程(有界面)"),
    ALL("所有进程"),
}