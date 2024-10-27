package com.venus.backgroundopt.xposed.manager.message.handle

import android.content.pm.PackageInfo
import com.venus.backgroundopt.common.util.UserUtils
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createJsonResponse
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * 查找其他用户([UserUtils.MAIN_USER]之外的用户)安装的app
 *
 * @author XingC
 * @date 2024/10/26
 */
object QueryOtherUserInstalledAppsMessageHandler : MessageHandler {
    override fun handle(runningInfo: RunningInfo, param: MethodHookParam, value: String?) {
        createJsonResponse<Any?>(
            param = param,
            value = value
        ) {
            runningInfo.packageManagerService?.getOtherUserInstalledApps() ?: emptyList<PackageInfo>()
        }
    }
}