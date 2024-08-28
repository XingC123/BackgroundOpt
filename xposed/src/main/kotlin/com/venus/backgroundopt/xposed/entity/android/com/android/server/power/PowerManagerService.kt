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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.power

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.power.compat.PowerManagerServiceSinceA14
import com.venus.backgroundopt.xposed.entity.android.com.android.server.power.compat.PowerManagerServiceUntilA13
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants

/**
 * 封装了[ClassConstants.PowerManagerService]
 *
 * @author XingC
 * @date 2024/8/27
 */
abstract class PowerManagerService(
    override val originalInstance: Any,
) : IEntityWrapper, IEntityCompatFlag {
    fun isGloballyInteractiveInternal(): Boolean = isGloballyInteractiveInternal(originalInstance)

    fun isInteractiveInternal(displayId: Int, uid: Int): Boolean = isInteractiveInternal(
        originalInstance, displayId, uid
    )

    object PowerManagerServiceHelper :
        IEntityCompatHelper<IPowerManagerService, PowerManagerService> {
        override val instanceClazz: Class<out PowerManagerService>
        override val instanceCreator: (Any) -> PowerManagerService
        override val compatHelperInstance: IPowerManagerService

        init {
            if (OsUtils.isUOrHigher) {
                instanceClazz = PowerManagerServiceSinceA14::class.java
                instanceCreator = ::createPowerManagerServiceSinceA14
                compatHelperInstance = PowerManagerServiceSinceA14.Companion
            } else {
                instanceClazz = PowerManagerServiceUntilA13::class.java
                instanceCreator = ::createPowerManagerServiceUntilA13
                compatHelperInstance = PowerManagerServiceUntilA13.Companion
            }
        }

        fun createPowerManagerServiceSinceA14(@OriginalObject powerManagerService: Any): PowerManagerService {
            return PowerManagerServiceSinceA14(powerManagerService)
        }

        fun createPowerManagerServiceUntilA13(@OriginalObject powerManagerService: Any): PowerManagerService {
            return PowerManagerServiceUntilA13(powerManagerService)
        }
    }

    companion object : IPowerManagerService {
        override fun isGloballyInteractiveInternal(instance: Any): Boolean {
            return PowerManagerServiceHelper.compatHelperInstance.isGloballyInteractiveInternal(
                instance
            )
        }

        override fun isInteractiveInternal(instance: Any, displayId: Int, uid: Int): Boolean {
            return PowerManagerServiceHelper.compatHelperInstance.isInteractiveInternal(
                instance,
                displayId,
                uid
            )
        }
    }
}

interface IPowerManagerService : IEntityCompatRule {
    fun isGloballyInteractiveInternal(@OriginalObject instance: Any): Boolean

    fun isInteractiveInternal(@OriginalObject instance: Any, displayId: Int, uid: Int): Boolean
}