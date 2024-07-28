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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.role

import com.venus.backgroundopt.xposed.annotation.OriginalMethod
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod

/**
 * 封装了[ClassConstants.RoleManagerService]
 *
 * @author XingC
 * @date 2024/7/15
 */
class RoleManagerService(
    override val originalInstance: Any,
) : IEntityWrapper {
    @OriginalMethod(returnTypePath = ClassConstants.RoleUserState)
    fun getOrCreateUserState(userId: Int): Any {
        return originalInstance.callMethod(MethodConstants.getOrCreateUserState, userId)!!
    }
}