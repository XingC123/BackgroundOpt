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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat

import android.app.role.RoleManager
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.annotation.OriginalObjectField
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ActivityManagerService
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.PackageManagerService
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.Settings
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.manager.application.DefaultApplicationManager
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getObjectFieldValue

/**
 * @author XingC
 * @date 2024/7/14
 */
class PackageManagerServiceCompatSinceA12(
    originalInstance: Any,
) : PackageManagerService(originalInstance) {
    @OriginalObjectField(fieldTypeClassPath = ClassConstants.PackageManagerServiceInjector)
    private val mInjector: Any? = originalInstance.getObjectFieldValue(FieldConstants.mInjector)

    @OriginalObject(classPath = ClassConstants.DefaultAppProvider)
    private val mDefaultAppProvider: Any? = mInjector?.callMethod(
        methodName = MethodConstants.getDefaultAppProvider
    )

    override fun getDefaultHome(): String? {
        return runCatchThrowable {
            mDefaultAppProvider?.callMethod<String?>(
                methodName = MethodConstants.getDefaultHome,
                ActivityManagerService.MAIN_USER
            )
        }
    }

    override fun getDefaultBrowser(): String? {
        return runCatchThrowable {
            mDefaultAppProvider?.callMethod<String?>(
                methodName = MethodConstants.getDefaultBrowser,
                ActivityManagerService.MAIN_USER
            )
        }
    }

    override fun getDefaultDialer(): String? {
        return runCatchThrowable {
            mDefaultAppProvider?.callMethod<String?>(
                methodName = MethodConstants.getDefaultDialer,
                ActivityManagerService.MAIN_USER
            )
        }
    }

    override fun getDefaultSms(): String? {
        return runCatchThrowable {
            mDefaultAppProvider?.callMethod<String?>(
                methodName = MethodConstants.getRoleHolder,
                RoleManager.ROLE_SMS,
                ActivityManagerService.MAIN_USER
            )
        }
    }

    override fun getDefaultAssistant(): String? {
        return DefaultApplicationManager.getDefaultPkgNameFromSettings(key = Settings.Secure.ASSISTANT)
    }

    override fun getDefaultInputMethod(): String? {
        return DefaultApplicationManager.getDefaultPkgNameFromSettings(key = Settings.Secure.DEFAULT_INPUT_METHOD)
    }
}