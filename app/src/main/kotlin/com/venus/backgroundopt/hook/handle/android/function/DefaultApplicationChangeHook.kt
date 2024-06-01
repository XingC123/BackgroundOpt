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

package com.venus.backgroundopt.hook.handle.android.function

import android.app.role.RoleManager
import android.content.ContentResolver
import com.venus.backgroundopt.annotation.FunctionHook
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.Settings
import com.venus.backgroundopt.manager.application.DefaultApplicationManager
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.concurrent.newThreadTask
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/3/27
 */
@FunctionHook(description = "监听默认应用的切换")
class DefaultApplicationChangeHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.RoleControllerManager.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.onAddRoleHolder,
            paramTypes = arrayOf(
                String::class.java,             /* roleName */
                String::class.java,             /* packageName */
                Int::class.java,                /* flags */
                ClassConstants.RemoteCallback   /* callback */
            )
        ) { param ->
            val roleName = param.args[0] as String
            newThreadTask {
                when (roleName) {
                    RoleManager.ROLE_HOME -> {
                        handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                            runningInfo.setDefaultPackageName(
                                DefaultApplicationManager.DEFAULT_APP_HOME,
                                packageName
                            )
                        }
                    }

                    RoleManager.ROLE_BROWSER -> {
                        handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                            runningInfo.setDefaultPackageName(
                                DefaultApplicationManager.DEFAULT_APP_BROWSER,
                                packageName
                            )
                        }
                    }

                    RoleManager.ROLE_SMS -> {
                        handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                            runningInfo.setDefaultPackageName(
                                DefaultApplicationManager.DEFAULT_APP_SMS,
                                packageName
                            )
                        }
                    }

                    RoleManager.ROLE_DIALER -> {
                        handleOnAddRoleHolder(param = param) { _, packageName, _, _ ->
                            runningInfo.setDefaultPackageName(
                                DefaultApplicationManager.DEFAULT_APP_DIALER,
                                packageName
                            )
                        }
                    }
                }
            }
        }

        ClassConstants.Settings_Secure.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.putStringForUser,
            paramTypes = arrayOf(
                ContentResolver::class.java,    /* resolver */
                String::class.java,             /* name */
                String::class.java,             /* value */
            ),
            hookAllMethod = true
        ) { param ->
            val name = param.args[1] as String
            val value = param.args[2] as? String ?: return@afterHook
            newThreadTask {
                when (name) {
                    Settings.Secure.ASSISTANT -> {
                        runningInfo.setDefaultPackageName(
                            DefaultApplicationManager.DEFAULT_APP_ASSISTANT,
                            value
                        )
                    }

                    Settings.Secure.DEFAULT_INPUT_METHOD -> {
                        runningInfo.setDefaultPackageName(
                            DefaultApplicationManager.DEFAULT_APP_INPUT_METHOD,
                            value
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private inline fun handleOnAddRoleHolder(
        param: XC_MethodHook.MethodHookParam,
        block: (String, String, Int, Any?) -> Unit
    ) {
        val roleName = param.args[0] as String
        val packageName = param.args[1] as String
        val flags = param.args[2] as Int
        val callback = param.args[3]
        block(roleName, packageName, flags, callback)
    }
}