package com.venus.backgroundopt.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
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

fun getTargetApps(context: Context, list: List<ProcessRecord>): List<AppItem> {
    val packageManager = context.packageManager

    return list.stream()
        .map { processRecord ->
            getPackageInfo(
                packageManager, processRecord.packageName,
                PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES
            )?.let { packageInfo ->
                val applicationInfo = packageInfo.applicationInfo
                AppItem(
                    applicationInfo.loadLabel(packageManager).toString(),
                    packageInfo.packageName,
                    applicationInfo.loadIcon(packageManager),
                    packageInfo
                ).apply {
                    pid = processRecord.pid
                }
            }
        }
        .filter(Objects::nonNull)
        .collect(Collectors.toList())
}
