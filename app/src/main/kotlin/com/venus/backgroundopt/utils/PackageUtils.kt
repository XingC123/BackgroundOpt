package com.venus.backgroundopt.utils

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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

const val PACKAGE_INFO_FLAG = 0
//    PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES    // 在某些系统可能存在获取不到app的情况

private fun getPackageInfo(
    packageManager: PackageManager,
    packageName: String,
    packageInfoFlag: Int = PACKAGE_INFO_FLAG
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

/**
 * 用于获取[AppItem]时部分信息的缓存
 *
 */
private class AppInfoCache {
    lateinit var appName: String
    lateinit var appIcon: Drawable
}

/**
 * 获取指定的应用的信息
 *
 * @param context 需要上下文来获取包管理器
 * @param list 要获取的应用的列表
 * @return 封装了应用名称/图标等信息的列表
 */
fun getTargetApps(context: Context, list: List<BaseProcessInfoKt>): List<AppItem> {
    val packageManager = context.packageManager
    // 应用部分信息缓存
    val infoCache = HashMap<String, AppInfoCache>()
    val appItems = arrayListOf<AppItem>()
    list.forEach { baseProcessInfo ->
        getPackageInfo(
            packageManager,
            baseProcessInfo.packageName
        )?.let { packageInfo ->
            val applicationInfo = packageInfo.applicationInfo ?: return@forEach
            // 保存应用信息
            val appInfo =
                infoCache.computeIfAbsent(baseProcessInfo.packageName) { _ ->
                    AppInfoCache().apply {
                        appName = applicationInfo.loadLabel(packageManager).toString()
                        appIcon = applicationInfo.loadIcon(packageManager)
                    }
                }
            val appItem = AppItem(
                appInfo.appName,
                baseProcessInfo.packageName,
                baseProcessInfo.uid,
                appInfo.appIcon,
                packageInfo
            ).apply {
                pid = baseProcessInfo.pid
                processName = baseProcessInfo.processName
                oomAdjScore = baseProcessInfo.oomAdjScore
                curAdj = baseProcessInfo.curAdj
                processingResult = baseProcessInfo.processingResult
            }
            appItems.add(appItem)
        }
    }
    // 清空缓存
    infoCache.clear()
    return appItems
}

/**
 * 获取已安装app的信息列表
 *
 * @param context 需要上下文来获取包管理器
 * @param filter 根据传入的条件过滤掉不需要的数据
 * @return 封装了应用名称/图标等信息的列表
 */
fun getInstalledPackages(
    context: Context,
    filter: ((PackageInfo) -> Boolean)? = null
): List<AppItem> {
    val packageManager = context.packageManager
    val packageInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_INFO_FLAG.toLong()))
    } else {
        packageManager.getInstalledPackages(PACKAGE_INFO_FLAG)
    }
    val list = arrayListOf<AppItem>()
    packageInfos.forEach { packageInfo ->
        packageInfo ?: return@forEach
        if (filter?.invoke(packageInfo) != true) {
            return@forEach
        }
        val applicationInfo = packageInfo.applicationInfo ?: return@forEach

        val appItem = AppItem(
            applicationInfo.loadLabel(packageManager).toString(),
            packageInfo.packageName,
            applicationInfo.uid,
            applicationInfo.loadIcon(packageManager),
            packageInfo
        ).apply {
            versionName = packageInfo.versionName
            longVersionCode = packageInfo.longVersionCode
        }
        list.add(appItem)
    }
    return list
}

fun getInstalledPackages(
    context: Context,
    appItems: MutableList<AppItem>
): List<AppItem> {
    val packageManager = context.packageManager

    appItems.forEach { appItem ->
        getPackageInfo(packageManager, appItem.packageName)?.let { packageInfo ->
            val applicationInfo = packageInfo.applicationInfo ?: return@forEach
            appItem.apply {
                this.appName = applicationInfo.loadLabel(packageManager).toString()
                this.uid = applicationInfo.uid
                this.appIcon = applicationInfo.loadIcon(packageManager)
                this.packageInfo = packageInfo
                this.versionName = packageInfo.versionName
                this.longVersionCode = packageInfo.longVersionCode
            }
        }
    }
    return appItems
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

