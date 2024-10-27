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

import android.content.pm.PackageInfo
import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat.PackageManagerServiceCompatA13
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat.PackageManagerServiceCompatSinceA12
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat.PackageManagerServiceCompatUntilA11
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
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
    val mUserManager: UserManagerService = RunningInfo.getInstance().userManagerService

    abstract fun getDefaultHome(): String?

    abstract fun getDefaultBrowser(): String?

    abstract fun getDefaultDialer(): String?

    abstract fun getDefaultSms(): String?

    abstract fun getDefaultAssistant(): String?

    abstract fun getDefaultInputMethod(): String?

    abstract fun getOtherUserInstalledApps(): List<PackageInfo>

    abstract fun getPackageInfoAsUser(
        packageName: String,
        userId: Int,
        packageInfoFlag: Int,
    ): PackageInfo?

    object PackageManagerServiceHelper :
        IEntityCompatHelper<IPackageManagerService, PackageManagerService> {
        override val instanceClazz: Class<out PackageManagerService>
        override val instanceCreator: (Any) -> PackageManagerService
        override val compatHelperInstance: IPackageManagerService

        init {
            if (OsUtils.isTOrHigher) {
                instanceClazz = PackageManagerServiceCompatA13::class.java
                compatHelperInstance = PackageManagerServiceCompatA13.Companion
                instanceCreator = ::createPackageManagerServiceA13
            } else if (OsUtils.isSOrHigher) {
                instanceClazz = PackageManagerServiceCompatSinceA12::class.java
                compatHelperInstance = PackageManagerServiceCompatSinceA12.Companion
                instanceCreator = ::createPackageManagerServiceSinceA12
            } else {
                instanceClazz = PackageManagerServiceCompatUntilA11::class.java
                compatHelperInstance = PackageManagerServiceCompatUntilA11.Companion
                instanceCreator = ::createPackageManagerServiceUntilA11
            }
        }

        private fun createPackageManagerServiceA13(@OriginalObject instance: Any): PackageManagerService =
            PackageManagerServiceCompatA13(instance)

        private fun createPackageManagerServiceSinceA12(@OriginalObject instance: Any): PackageManagerService =
            PackageManagerServiceCompatSinceA12(instance)

        private fun createPackageManagerServiceUntilA11(@OriginalObject instance: Any): PackageManagerService =
            PackageManagerServiceCompatUntilA11(instance)
    }
}

interface IPackageManagerService : IEntityCompatRule