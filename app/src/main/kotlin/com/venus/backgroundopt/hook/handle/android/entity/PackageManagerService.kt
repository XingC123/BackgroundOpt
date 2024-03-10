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
                    
 package com.venus.backgroundopt.hook.handle.android.entity

import com.venus.backgroundopt.annotation.AndroidObject
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.runCatchThrowable

/**
 * @author XingC
 * @date 2024/2/29
 */
class PackageManagerService(
    @AndroidObject(classPath = ClassConstants.PackageManagerService)
    override val originalInstance: Any
) : IAndroidEntity {
    fun getDefaultHome(): String? {
        return runCatchThrowable(defaultValue = null) {
            originalInstance.getObjectFieldValue(fieldName = FieldConstants.mInjector)?.let { mInjector->
                mInjector.callMethod(methodName = MethodConstants.getDefaultAppProvider)?.let { mDefaultAppProvider->
                    mDefaultAppProvider.callMethod(methodName = MethodConstants.getDefaultHome, 0) as String?
                }
            }
        }
    }
}