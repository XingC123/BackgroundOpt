package com.venus.backgroundopt.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfo
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
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, packageInfoFlag)
    } else {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(packageInfoFlag.toLong())
        )
    }
}

fun getTargetApps(context: Context, list: List<BaseProcessInfo>): List<AppItem> {
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
                    packageInfo.packageName,
                    applicationInfo.loadIcon(packageManager),
                    packageInfo
                ).apply {
                    pid = baseProcessInfo.pid
                    processName = baseProcessInfo.processName ?: null
                }
            }
        }
        .filter(Objects::nonNull)
        .collect(Collectors.toList())
}
