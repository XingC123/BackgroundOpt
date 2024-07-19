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
                    
package com.venus.backgroundopt.xposed.hook

import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.MethodHook

/**
 * @author XingC
 * @date 2023/10/17
 */
open class MethodHookKt(
    classLoader: ClassLoader,
    runningInfo: RunningInfo
) : MethodHook(classLoader, runningInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf()
    }
}