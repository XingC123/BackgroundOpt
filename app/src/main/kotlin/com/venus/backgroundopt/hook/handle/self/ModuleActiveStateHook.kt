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
                    
 package com.venus.backgroundopt.hook.handle.self

import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.base.generateHookPoint
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/29
 */
class ModuleActiveStateHook(classLoader: ClassLoader?) : MethodHook(classLoader) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            generateHookPoint(
                true,
                "com.venus.backgroundopt.environment.CommonProperties",
                "isModuleActive",
                arrayOf(
                    beforeHookAction { handleModuleActiveState(it) }
                )
            ),
        )
    }

    /**
     * 改变模块激活状态
     *
     * @param param
     */
    private fun handleModuleActiveState(param: MethodHookParam) {
        param.result = true
    }
}