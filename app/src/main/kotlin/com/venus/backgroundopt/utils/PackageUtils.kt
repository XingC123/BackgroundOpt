package com.venus.backgroundopt.utils

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.utils.log.logErrorAndroid
import java.util.Objects
import java.util.stream.Collectors

/**
 * @author XingC
 * @date 2023/9/25
 */

const val processNameSeparator: String = ":"

/**
 * 绝对进程名
 *
 * @param packageName 包名
 * @param processName 进程名
 */
fun absoluteProcessName(packageName: String, processName: String): String {
    // 相对进程名
    return if (processName.startsWith(".")) {
        // 拼成绝对进程名
        "${packageName}${processNameSeparator}${processName.substring(1)}"
    } else {
        // 是绝对进程直接返回
        processName
    }
}

const val PACKAGE_INFO_FLAG =
    PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES

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
                packageManager,
                baseProcessInfo.packageName,
                PACKAGE_INFO_FLAG
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
                    oomAdjScore = baseProcessInfo.oomAdjScore
                    curAdj = baseProcessInfo.curAdj
                }
            }
        }
        .filter(Objects::nonNull)
        .collect(Collectors.toList())
}

fun getInstalledPackages(
    context: Context,
    filter: ((PackageInfo) -> Boolean)? = null
): List<AppItem> {
    val packageManager = context.packageManager
    val packageInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(
                PACKAGE_INFO_FLAG.toLong()
            )
        )
    } else {
        packageManager.getInstalledPackages(PACKAGE_INFO_FLAG)
    }

    return packageInfos.stream()
        .filterNullable(filter)
        .map { packageInfo ->
            val applicationInfo = packageInfo.applicationInfo
            AppItem(
                applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                applicationInfo.uid,
                applicationInfo.loadIcon(packageManager),
                packageInfo
            ).apply {
                versionName = packageInfo.versionName
                longVersionCode = packageInfo.longVersionCode
            }
        }
        .collect(Collectors.toList())
}

fun getAppProcesses(context: Context, appItem: AppItem) {
    try {
        val packageManager = context.packageManager
        val packageInfo: PackageInfo = packageManager.getPackageInfo(
            appItem.packageName,
            PackageManager.MATCH_UNINSTALLED_PACKAGES or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS
        )

        appItem.processes = linkedSetOf<String>().apply {
            packageInfo.activities?.let { addProcess(this, it) }
            packageInfo.services?.let { addProcess(this, it) }
            packageInfo.receivers?.let { addProcess(this, it) }
            packageInfo.providers?.let { addProcess(this, it) }
        }
    } catch (t: Throwable) {
        logErrorAndroid(
            "fun getAppProcesses(appItem: AppItem)",
            "获取App(${appItem.packageName})清单文件中进程失败",
            t
        )
    }
}

private fun addProcess(set: MutableSet<String>, componentInfos: Array<out ComponentInfo>) {
    componentInfos.forEach { componentInfo ->
        set.add(absoluteProcessName(componentInfo.packageName, componentInfo.processName))
    }
}

