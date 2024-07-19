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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.pm

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat.PackageManagerServiceCompatSinceA12
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat.PackageManagerServiceCompatUntilA11
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants

/**
 * 封装了安卓的[ClassConstants.PackageManagerService]
 *
 * @author XingC
 * @date 2024/2/29
 */
abstract class PackageManagerService(
    @OriginalObject(classPath = ClassConstants.PackageManagerService)
    final override val originalInstance: Any,
) : IEntityWrapper, IEntityCompatFlag {
    abstract fun getDefaultHome(): String?

    abstract fun getDefaultBrowser(): String?

    abstract fun getDefaultDialer(): String?

    abstract fun getDefaultSms(): String?

    abstract fun getDefaultAssistant(): String?

    abstract fun getDefaultInputMethod(): String?

    object PackageManagerServiceHelper : IEntityCompatHelper<PackageManagerService> {
        override val instanceClazz: Class<out PackageManagerService>
        override val instanceCreator: (Any) -> PackageManagerService

        init {
            if (OsUtils.isSOrHigher) {
                instanceClazz = PackageManagerServiceCompatSinceA12::class.java
                instanceCreator = ::createPackageManagerServiceSinceA12
            } else {
                instanceClazz = PackageManagerServiceCompatUntilA11::class.java
                instanceCreator = ::createPackageManagerServiceUntilA11
            }
        }

        private fun createPackageManagerServiceSinceA12(@OriginalObject instance: Any): PackageManagerService =
            PackageManagerServiceCompatSinceA12(instance)

        private fun createPackageManagerServiceUntilA11(@OriginalObject instance: Any): PackageManagerService =
            PackageManagerServiceCompatUntilA11(instance)
    }
}