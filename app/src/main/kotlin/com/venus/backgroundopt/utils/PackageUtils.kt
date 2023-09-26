package com.venus.backgroundopt.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import java.util.Objects
import java.util.stream.Collectors

/**
 * @author XingC
 * @date 2023/9/25
 */

private fun getPackageInfo(
    packageManager: PackageManager,
    packageName: String,
    packageInfoFlag: Int
): PackageInfo? {
    return try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, packageInfoFlag)
        } else {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(packageInfoFlag.toLong())
            )
        }
    } catch (t: Throwable) {
        null
    }
}

fun getTargetApps(context: Context, list: List<BaseProcessInfoKt>): List<AppItem> {
    val packageManager = context.packageManager

    return list.stream()
        .map { baseProcessInfo ->
            getPackageInfo(
                packageManager, baseProcessInfo.packageName,
                PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES
            )?.let { packageInfo ->
                val applicationInfo = packageInfo.applicationInfo
                AppItem(
                    applicationInfo.loadLabel(packageManager).toString(),
                    baseProcessInfo.packageName,
                    baseProcessInfo.uid,
                    applicationInfo.loadIcon(packageManager),
                    packageInfo
                ).apply {
                    pid = baseProcessInfo.pid
                    processName = baseProcessInfo.processName
                }
            }
        }
        .filter(Objects::nonNull)
        .collect(Collectors.toList())
}
