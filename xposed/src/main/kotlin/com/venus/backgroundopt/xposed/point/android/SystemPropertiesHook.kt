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

package com.venus.backgroundopt.xposed.point.android

import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.action.beforeHookAction
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.MethodHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/27
 */
class SystemPropertiesHook(
    classLoader: ClassLoader,
    hookInfo: RunningInfo,
) : MethodHook(classLoader, hookInfo), ILogger {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.SystemProperties,
                MethodConstants.getBoolean,
                arrayOf(
                    beforeHookAction {
                        handleGetBoolean(it)
                    }
                ),
                String::class.java,
                Boolean::class.java
            )
        )
    }

    private fun logPrefix(): String = "SystemProperties: "

    private fun handleGetBoolean(param: MethodHookParam) {
        when (param.args[0] as String) {
            "persist.sys.spc.enabled" -> {
                logger.debug("${logPrefix()}persist.sys.spc.enabled >>> return false")
                param.result = false
            }
        }
    }
}