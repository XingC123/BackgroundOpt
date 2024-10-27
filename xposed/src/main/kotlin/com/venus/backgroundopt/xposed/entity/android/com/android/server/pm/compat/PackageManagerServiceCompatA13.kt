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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.compat

import android.content.pm.PackageInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.IPackageManagerService
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod

/**
 * @author XingC
 * @date 2024/10/27
 */
class PackageManagerServiceCompatA13(
    originalInstance: Any,
) : PackageManagerServiceCompatSinceA12(originalInstance) {
    override fun getPackageInfoAsUser(
        packageName: String,
        userId: Int,
        packageInfoFlag: Int
    ): PackageInfo? {
        return mLiveComputer.callMethod<PackageInfo?>(
            MethodConstants.getPackageInfo,
            packageName,
            packageInfoFlag,
            userId
        )
    }

    companion object : IPackageManagerService by PackageManagerServiceCompatSinceA12
}