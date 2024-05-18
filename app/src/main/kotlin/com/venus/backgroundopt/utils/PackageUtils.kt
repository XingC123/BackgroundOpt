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

package com.venus.backgroundopt.utils

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.ui.ConfigureAppProcessActivity
import com.venus.backgroundopt.utils.log.logErrorAndroid
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.preference.prefAll
import java.text.Collator
import java.util.Locale

/**
 * @author XingC
 * @date 2023/9/25
 */
object PackageUtils {
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

    const val PACKAGE_INFO_FLAG = 0 or PackageManager.GET_ACTIVITIES
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

        val appItems = list.asSequence()
            .map { baseProcessInfo ->
                getPackageInfo(
                    packageManager,
                    baseProcessInfo.packageName
                )?.let { packageInfo ->
                    val applicationInfo = packageInfo.applicationInfo ?: return@map null
                    // 保存应用信息
                    val appInfo =
                        infoCache.computeIfAbsent(baseProcessInfo.packageName) { _ ->
                            AppInfoCache().apply {
                                appName = applicationInfo.loadLabel(packageManager).toString()
                                appIcon = applicationInfo.loadIcon(packageManager)
                            }
                        }
                    AppItem(
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
                        rssInBytes = baseProcessInfo.rssInBytes
                        lastProcessingResultMap = baseProcessInfo.lastProcessingResultMap
                    }
                }
            }
            .filterNotNull()
            .toList()
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
    fun getTargetApps(context: Context, packageNames: Collection<String>): List<AppItem> {
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
            .toList()
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

        // 获取配置, 以app是否设置过做附加排序
        val subProcessOomPolicyMap =
            context.prefAll<SubProcessOomPolicy>(PreferenceNameConstants.SUB_PROCESS_OOM_POLICY)
        val appOptimizePolicies =
            context.prefAll<AppOptimizePolicy>(PreferenceNameConstants.APP_OPTIMIZE_POLICY)

        fun hasConfiguredApp(appItem: AppItem): Boolean {
            val packageName = appItem.packageName
            // app是否启用优化
            var hasConfiguredAppOptimizePolicy = false
            // 自定义oom
            var hasConfiguredMainProcessCustomOomScore = false
            // 管理系统app的主进程的adj
            var hasConfiguredShouldHandleMainProcAdj = false
            // 主进程ADJ管理策略
            var hasConfiguredMainProcessAdjManagePolicy = false
            appOptimizePolicies[packageName]?.let { appOptimizePolicy ->
                if (appOptimizePolicy.disableForegroundTrimMem != null ||
                    appOptimizePolicy.disableBackgroundTrimMem != null ||
                    appOptimizePolicy.disableBackgroundGc != null
                ) {
                    // 正在使用旧版配置
                    // 保存新版参数
                    ConfigureAppProcessActivity.saveAppMemoryOptimize(appOptimizePolicy, context)
                }

                if (appOptimizePolicy.enableForegroundTrimMem == !PreferenceDefaultValue.enableForegroundTrimMem ||
                    appOptimizePolicy.enableBackgroundTrimMem == !PreferenceDefaultValue.enableBackgroundTrimMem ||
                    appOptimizePolicy.enableBackgroundGc == !PreferenceDefaultValue.enableBackgroundGc
                ) {
                    appItem.appConfiguredEnumSet.add(AppItem.AppConfiguredEnum.AppOptimizePolicy)
                    hasConfiguredAppOptimizePolicy = true
                }

                if (appOptimizePolicy.enableCustomMainProcessOomScore) {
                    appItem.appConfiguredEnumSet.add(AppItem.AppConfiguredEnum.CustomMainProcessOomScore)
                    hasConfiguredMainProcessCustomOomScore = true
                }

                // 系统app 且 允许管理oom adj
                /*if (appOptimizePolicy.shouldHandleAdj == true) {
                    appItem.appConfiguredEnumSet.add(AppItem.AppConfiguredEnum.ShouldHandleMainProcAdj)
                    hasConfiguredShouldHandleMainProcAdj = true
                }*/

                // 主进程ADJ管理策略
                if (appOptimizePolicy.mainProcessAdjManagePolicy != AppOptimizePolicy.MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_DEFAULT) {
                    appItem.appConfiguredEnumSet.add(AppItem.AppConfiguredEnum.MainProcessAdjManagePolicy)
                    hasConfiguredMainProcessAdjManagePolicy = true
                }
            }

            // 是否配置过子进程oom策略
            var hasConfiguredSubProcessOomPolicy = false
            subProcessOomPolicyMap.forEach { (subProcessName, subProcessOomPolicy) ->
                if (subProcessName.contains(packageName)) {
                    if (subProcessOomPolicy.policyEnum != SubProcessOomPolicy.SubProcessOomPolicyEnum.DEFAULT) {
                        appItem.appConfiguredEnumSet.add(AppItem.AppConfiguredEnum.SubProcessOomPolicy)
                        hasConfiguredSubProcessOomPolicy = true
                        return@forEach
                    }
                }
            }
            return hasConfiguredAppOptimizePolicy or
                    hasConfiguredMainProcessCustomOomScore or
                    hasConfiguredShouldHandleMainProcAdj or
                    hasConfiguredMainProcessAdjManagePolicy or
                    hasConfiguredSubProcessOomPolicy
        }

        // 排序器
        val appNameComparator = object : Comparator<AppItem> {
            val chinaCollator = Collator.getInstance(Locale.CHINA)
            fun isEnglish(char: Char): Boolean {
                return char in 'a'..'z' || char in 'A'..'z'
            }

            /**
             * 最终排序为: 前: 所有英文的app名字。后: 所有中文的app名字
             */
            override fun compare(o1: AppItem, o2: AppItem): Int {
                val firstAppName = o1.appName
                val secondAppName = o2.appName
                // 只判断app名字的第一个字符(英文: 首字母。中文: 第一个字)
                val isFirstNameEnglish = isEnglish(firstAppName[0])
                val isSecondNameEnglish = isEnglish(secondAppName[0])
                if (isFirstNameEnglish) {
                    if (isSecondNameEnglish) {
                        // 都是英文
                        return o1.appName.lowercase().compareTo(o2.appName.lowercase())
                    }
                    // 第一个是英文, 第二个是中文。
                    return -1
                } else if (!isSecondNameEnglish) {
                    // 两个都是中文
                    return chinaCollator.compare(firstAppName, secondAppName)
                }

                return 1
            }
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
                    systemApp = ActivityManagerService.isImportantSystemApp(applicationInfo)
                }
            }
            .sortedWith(
                Comparator.comparing(::hasConfiguredApp)
                    .thenComparator { appItem1, appItem2 ->
                        appNameComparator.compare(
                            appItem2,
                            appItem1
                        )
                    }
                    .reversed())
            .toList()
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