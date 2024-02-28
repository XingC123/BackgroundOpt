package com.venus.backgroundopt.entity

import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.hook.handle.android.entity.ApplicationInfo

/**
 * @author XingC
 * @date 2024/2/18
 */
class FindAppResult() {
    var importantSystemApp: Boolean = false
    var hasActivity: Boolean = false
    var applicationInfo: ApplicationInfo? = null

    constructor(applicationInfo: ApplicationInfo?) : this() {
        this.applicationInfo = applicationInfo

        applicationInfo?.let {
            importantSystemApp =
                ActivityManagerService.isImportantSystemApp(applicationInfo.applicationInfo)
        }
    }
}