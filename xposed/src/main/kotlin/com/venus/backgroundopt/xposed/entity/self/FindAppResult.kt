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

package com.venus.backgroundopt.xposed.entity.self

import com.venus.backgroundopt.xposed.entity.android.android.content.pm.ApplicationInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ActivityManagerService

/**
 * @author XingC
 * @date 2024/2/18
 */
class FindAppResult() {
    var importantSystemApp: Boolean = false
    var hasActivity: Boolean = false
    var applicationInfo: ApplicationInfo? = null
    var appInfo: AppInfo? = null

    constructor(applicationInfo: ApplicationInfo?) : this() {
        this.applicationInfo = applicationInfo

        applicationInfo?.let {
            importantSystemApp = ActivityManagerService.isImportantSystemApp(
                applicationInfo.applicationInfo
            )
        }
    }

    constructor(applicationInfo: android.content.pm.ApplicationInfo?) : this() {
        applicationInfo?.let {
            this.applicationInfo = ApplicationInfo(applicationInfo)
            this.importantSystemApp = ActivityManagerService.isImportantSystemApp(
                applicationInfo
            )
        }
    }
}

inline fun FindAppResult.getOrCreateAppInfo(block: (FindAppResult) -> AppInfo): AppInfo {
    return (appInfo ?: run {
        block(this).also { appInfo = it }
    }).apply {
        init()
    }
}
