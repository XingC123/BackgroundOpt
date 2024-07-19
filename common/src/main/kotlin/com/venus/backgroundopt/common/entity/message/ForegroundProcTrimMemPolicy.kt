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

import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.util.message.MessageFlag
import com.venus.backgroundopt.xposed.entity.self.ComponentCallbacks2Constants

/**
 * @author XingC
 * @date 2023/11/3
 */
class ForegroundProcTrimMemPolicy : MessageFlag {
    var isEnabled = PreferenceDefaultValue.enableForegroundTrimMem
    var foregroundProcTrimMemLevelEnum = ForegroundProcTrimMemLevelEnum.RUNNING_MODERATE
}

enum class ForegroundProcTrimMemLevelEnum(val uiName: String, val level: Int) : MessageFlag {
    RUNNING_MODERATE("系统内存稍低", ComponentCallbacks2Constants.TRIM_MEMORY_RUNNING_MODERATE),
    RUNNING_LOW("系统内存相当低", ComponentCallbacks2Constants.TRIM_MEMORY_RUNNING_LOW),
    RUNNING_CRITICAL("系统内存不足", ComponentCallbacks2Constants.TRIM_MEMORY_RUNNING_CRITICAL),
    UI_HIDDEN("UI不可见", ComponentCallbacks2Constants.TRIM_MEMORY_UI_HIDDEN),
}