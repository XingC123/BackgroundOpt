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

package com.venus.backgroundopt.common.entity.message

import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.common.entity.preference.JsonPreferenceFlag
import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.util.message.MessageFlag

/**
 * @author XingC
 * @date 2023/11/5
 */
class AppOptimizePolicy : MessageFlag, JsonPreferenceFlag {
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
    var customMainProcessFgAdj = Int.MIN_VALUE      /* 进程处于前台时的adj */
    var customMainProcessOomScore = Int.MIN_VALUE   /* 进程处于后台时的adj */

    // 该app是否管理adj
    @Deprecated(message = "已被取代", replaceWith = ReplaceWith("mainProcessAdjManagePolicy"))
    @JSONField(serialize = false)
    var shouldHandleAdj: Boolean? = null
        set(value) {
            if (value == true) {
                mainProcessAdjManagePolicy = MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_ALWAYS
            } else if (value == false) {
                mainProcessAdjManagePolicy = MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_NEVER
            }
            field = value
        }

    // 在ui中的开关应该显示的状态
    // 增加此属性, 仅通过此属性决定ui的状态, 而不通过shouldHandleAdj,
    // 防止因版本变化导致的后端策略的变化从而使用户侧存储了错误数据(用户从未设置过此字段, 却被模块后端影响, 并持久化)
    // 该属性仅通过模块后端决定, 不在shouldHandleAdj改变的时候在用户侧修改
    @Deprecated(message = "已被取代", replaceWith = ReplaceWith("mainProcessAdjManagePolicy"))
    @JSONField(serialize = false)
    var shouldHandleAdjUiState: Boolean = false

    /**
     * 有界面时保活主进程
     *
     * 生效逻辑详见: [ProcessListHookKt.applyHighPriorityProcessFinalAdj]
     */
    @Deprecated(message = "已被取代", replaceWith = ReplaceWith("mainProcessAdjManagePolicy"))
    var keepMainProcessAliveHasActivity: Boolean? = null

    // 主进程ADJ管理策略
    var mainProcessAdjManagePolicy = MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_DEFAULT
    var defaultMainProcessAdjManagePolicyUiText = "invalid"

    enum class MainProcessAdjManagePolicy(val code: Int, val uiText: String) {
        MAIN_PROC_ADJ_MANAGE_DEFAULT(0, "重置"),
        MAIN_PROC_ADJ_MANAGE_NEVER(1, "从不"),
        MAIN_PROC_ADJ_MANAGE_HAS_ACTIVITY(2, "拥有界面"),
        MAIN_PROC_ADJ_MANAGE_ALWAYS(3, "强制"),
    }
}

class AppOptimizePolicyMessage : MessageFlag {
    var value: Any? = null
    var messageType = MSG_NONE

    var uid: Int = 0
    var packageName: String = ""

    companion object {
        const val MSG_NONE = 0
        const val MSG_CREATE_OR_GET = 1
        const val MSG_SAVE = 2
    }
}