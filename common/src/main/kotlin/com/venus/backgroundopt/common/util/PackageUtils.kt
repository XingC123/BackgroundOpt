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

package com.venus.backgroundopt.common.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.util.log.logErrorAndroid
import com.venus.backgroundopt.xposed.entity.self.ProcessRecordBaseInfo

/**
 * @author XingC
 * @date 2023/9/25
 */
object PackageUtils {
    const val processNameSeparator: String = ":"

    const val MAIN_USER: Int = 0

    // 用户安装的app的uid大于10000
    const val USER_APP_UID_START_NUM: Int = 10000

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
            "${packageName}$processNameSeparator${processName.substring(1)}"
        } else {
            // 是绝对进程直接返回
            processName
        }
    }

    @JvmStatic
    fun isImportantSystemApp(applicationInfo: ApplicationInfo?): Boolean {
        return applicationInfo == null
                || applicationInfo.uid < USER_APP_UID_START_NUM
                || (applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
    }

    const val PACKAGE_INFO_FLAG = 0 // or PackageManager.GET_ACTIVITIES
//    PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.GET_ACTIVITIES    // 在某些系统可能存在获取不到app的情况

    private fun getPackageInfo(
        packageManager: PackageManager,
        packageName: String,
        packageInfoFlag: Int = PACKAGE_INFO_FLAG,
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
    fun getTargetApps(context: Context, list: List<ProcessRecordBaseInfo>): MutableList<AppItem> {
        val packageManager = context.packageManager
        // 应用部分信息缓存
        val infoCache = HashMap<String, AppInfoCache>()

        val appItems = list.asSequence()
            .map { processRecordBaseInfo ->
                getPackageInfo(
                    packageManager,
                    processRecordBaseInfo.packageName
                )?.let { packageInfo ->
                    val applicationInfo = packageInfo.applicationInfo ?: return@map null
                    // 保存应用信息
                    val appInfo =
                        infoCache.computeIfAbsent(processRecordBaseInfo.packageName) { _ ->
                            AppInfoCache().apply {
                                appName = applicationInfo.loadLabel(packageManager).toString()
                                appIcon = applicationInfo.loadIcon(packageManager)
                            }
                        }
                    AppItem(
                        appInfo.appName,
                        processRecordBaseInfo.packageName,
                        processRecordBaseInfo.uid,
                        appInfo.appIcon,
                        packageInfo
                    ).apply {
                        pid = processRecordBaseInfo.pid
                        processName = processRecordBaseInfo.processName
                        fullQualifiedProcessName =
                            absoluteProcessName(packageName, processRecordBaseInfo.processName)
                        oomAdjScore = processRecordBaseInfo.oomAdjScore
                        curAdj = processRecordBaseInfo.curAdj
                        rssInBytes = processRecordBaseInfo.rssInBytes
                        systemApp = isImportantSystemApp(applicationInfo)
                        lastProcessingResultMap = processRecordBaseInfo.lastProcessingResultMap
                    }
                }
            }
            .filterNotNull()
            .toMutableList()
        // 清空缓存
        infoCache.clear()
        return appItems
    }

    /**
     * 根据[packageNames]包名, 查找匹配的[AppItem]集合
     * @param context Context
     * @param packageNames Collection<String>
     * @return List<AppItem>
     */
    fun getTargetApps(context: Context, packageNames: Collection<String>): MutableList<AppItem> {
        val packageManager = context.packageManager
        return packageNames.asSequence()
            .map { packageName ->
                getPackageInfo(
                    packageManager,
                    packageName
                )?.let { packageInfo ->
                    val applicationInfo = packageInfo.applicationInfo ?: return@map null
                    AppItem(packageName).apply {
                        appName = applicationInfo.loadLabel(packageManager).toString()
                        appIcon = applicationInfo.loadIcon(packageManager)
                        this.packageInfo = packageInfo
                    }
                }
            }
            .filterNotNull()
            .toMutableList()
    }

    fun getSelfInfo(context: Context): AppItem = getTargetApps(
        context,
        listOf(CommonProperties.PACKAGE_NAME)
    )[0]

    /**
     * 获取已安装app的信息列表
     *
     * @param context 需要上下文来获取包管理器
     * @param filter 根据传入的条件过滤掉不需要的数据
     * @return 封装了应用名称/图标等信息的列表
     */
    fun getInstalledPackages(
        context: Context,
        filter: ((PackageInfo) -> Boolean)? = null,
    ): MutableList<AppItem> {
        val packageManager = context.packageManager
        val packageInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_INFO_FLAG.toLong()))
        } else {
            packageManager.getInstalledPackages(PACKAGE_INFO_FLAG)
        }

        return packageInfos.asSequence()
            .filterNotNull()
            .nullableFilter(filter)
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
                    systemApp = isImportantSystemApp(applicationInfo)
                }
            }
            .toMutableList()
    }

    fun getInstalledPackages(
        context: Context,
        appItems: MutableList<AppItem>,
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

    /**
     * 给定的app是否有界面
     *
     * @param packageInfo PackageInfo 包信息
     * @return Boolean 有界面 -> true
     */
    fun isHasActivity(packageInfo: PackageInfo): Boolean {
        return !packageInfo.activities.isNullOrEmpty()
    }
}