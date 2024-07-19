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

import android.app.role.RoleManager
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
@Deprecated(message = "被取代", ReplaceWith(expression = "DefaultApplicationChangeHook"))
class RoleControllerManagerHook(
    classLoader: ClassLoader,
    hookInfo: RunningInfo,
) : MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.RoleControllerManager,
                MethodConstants.onAddRoleHolder,
                arrayOf(
                    beforeHookAction {
                        handleOnAddRoleHolder(it)
                    }
                ),
                String::class.java,             /* roleName */
                String::class.java,             /* packageName */
                Int::class.java,                /* flags */
                ClassConstants.RemoteCallback   /* callback */
            )
        )
    }

    private fun handleOnAddRoleHolder(param: MethodHookParam) {
        when (param.args[0] as String) {
            RoleManager.ROLE_HOME -> {
                handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                    runningInfo.setDefaultPackageName(RoleManager.ROLE_HOME, packageName)
                }
            }

            RoleManager.ROLE_BROWSER -> {
                handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                    runningInfo.setDefaultPackageName(RoleManager.ROLE_BROWSER, packageName)
                }
            }

            RoleManager.ROLE_ASSISTANT -> {
                handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                    runningInfo.setDefaultPackageName(RoleManager.ROLE_ASSISTANT, packageName)
                }
            }
        }
    }

    private inline fun handleOnAddRoleHolder(
        param: MethodHookParam,
        block: (String, String, Int, Any?) -> Unit,
    ) {
        val roleName = param.args[0] as String
        val packageName = param.args[1] as String
        val flags = param.args[2] as Int
        val callback = param.args[3]
        block(roleName, packageName, flags, callback)
    }
}