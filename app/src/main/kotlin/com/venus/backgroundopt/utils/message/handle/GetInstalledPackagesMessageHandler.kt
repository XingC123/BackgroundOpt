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
                    
 package com.venus.backgroundopt.utils.message.handle

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 *  获取已安装app的消息处理器
 *  从UI界面使用packageManager.getInstalledPackages有可能无法读取全部app, 因此先请求到此, 梳理一个列表后进行返回再做其他操作
 *
 * @author XingC
 * @date 2023/10/2
 */
class GetInstalledPackagesMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<Any>(param, value, setJsonData = true) { _ ->
            val packageManager = runningInfo.activityManagerService.packageManager
            val packageInfos = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                XposedHelpers.callMethod(
                    packageManager,
                    MethodConstants.getInstalledPackages,
                    PackageManager.PackageInfoFlags.of(PackageUtils.PACKAGE_INFO_FLAG.toLong())
                )
            } else {
                XposedHelpers.callMethod(
                    packageManager,
                    MethodConstants.getInstalledPackages,
                    PackageUtils.PACKAGE_INFO_FLAG
                )
            }) as? List<*>

            val finalList = arrayListOf<AppItem>()
            packageInfos?.forEach {
                val packageInfo = it as PackageInfo
                val applicationInfo = packageInfo.applicationInfo
                if (ActivityManagerService.isImportantSystemApp(applicationInfo)) {
                    return@forEach
                }
                finalList.add(AppItem(packageInfo.packageName))
            }
            finalList
        }
    }
}