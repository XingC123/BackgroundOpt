/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.power.compat

import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.power.IPowerManagerService
import com.venus.backgroundopt.xposed.entity.android.com.android.server.power.PowerManagerService
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod
import java.lang.UnsupportedOperationException

/**
 * @author XingC
 * @date 2024/8/27
 */
class PowerManagerServiceUntilA13(
    originalInstance: Any,
) : PowerManagerService(originalInstance) {
    companion object : IPowerManagerService {
        @IEntityCompatMethod
        override fun isGloballyInteractiveInternal(@OriginalObject instance: Any): Boolean {
            return instance.callMethod<Boolean>(MethodConstants.isInteractiveInternal)
        }

        @IEntityCompatMethod
        override fun isInteractiveInternal(
            @OriginalObject instance: Any,
            displayId: Int,
            uid: Int,
        ): Boolean {
            throw UnsupportedOperationException()
        }
    }
}