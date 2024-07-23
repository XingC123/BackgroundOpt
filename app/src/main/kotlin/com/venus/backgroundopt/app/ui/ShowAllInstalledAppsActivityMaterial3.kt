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

package com.venus.backgroundopt.app.ui

import android.content.ClipData
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.BaseActivityMaterial3
import com.venus.backgroundopt.app.ui.base.SearchFromAppItemRVAdapter
import com.venus.backgroundopt.app.ui.style.RecycleViewItemSpaceDecoration
import com.venus.backgroundopt.app.utils.SystemServices
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.app.utils.showProgressBarViewForAction
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.entity.AppItem.AppConfiguredEnum
import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy.SubProcessOomPolicyEnum
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.common.util.PackageUtils
import com.venus.backgroundopt.common.util.containsIgnoreCase
import com.venus.backgroundopt.common.util.ifTrue
import com.venus.backgroundopt.common.util.log.logInfoAndroid
import com.venus.backgroundopt.common.util.preference.prefAll
import com.venus.backgroundopt.common.util.runCatchThrowable
import java.text.Collator
import java.util.Locale


/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsActivityMaterial3 : BaseActivityMaterial3() {
    lateinit var subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>
    lateinit var appOptimizePolicies: MutableMap<String, AppOptimizePolicy>

    private lateinit var appItems: MutableList<AppItem>
    private var isAllowedRefreshInstalledAppsUi: Boolean = false

    private lateinit var appItemsRecyclerViewAdapter: ShowAllInstalledAppsAdapter3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction("正在获取已安装应用...") {
            init()
        }
    }

    override fun onResume() {
        super.onResume()

        isAllowedRefreshInstalledAppsUi.ifTrue {
            showProgressBarViewForAction(text = "布局更新中...") {
                sortAppList()
                runOnUiThread {
                    appItemsRecyclerViewAdapter.refreshInstalledAppsListUI()
                }
                isAllowedRefreshInstalledAppsUi = false
            }
        }
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_all_installed_apps_material3
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.installerAppToolbarHelpMenuItem -> {
                UiUtils.createDialog(
                    context = this,
                    viewResId = R.layout.content_installed_app_toolbar_help
                ).show()
            }
        }
    }

    private fun init() {
        // app展示区
        val recyclerView = findViewById<RecyclerView>(R.id.showAllInstalledAppsRecycleView)
        // 搜索历史
        val searchHistoryContainer =
            findViewById<RecyclerView>(R.id.showAllInstalledAppsSearchHistoryRecycleView)

        // 初始化搜索栏
        val appNameSearchBar = findViewById<SearchBar>(R.id.showAllInstalledAppsSearchBar)
        fun applySearch(text: CharSequence, searchView: SearchView) {
            appNameSearchBar.setText(text)
            searchView.hide()
            (recyclerView.adapter as ShowAllInstalledAppsAdapter3).filter.filter(text)
        }

        val searchView = findViewById<SearchView>(R.id.showAllInstalledAppsSearchView).apply {
            editText.setOnEditorActionListener { v, actionId, event ->
                event?.let { e ->
                    if (e.keyCode == KeyEvent.KEYCODE_ENTER) {
                        applySearch(v.text, this)
                    }
                }
                false
            }

            editText.addTextChangedListener {
                (searchHistoryContainer.adapter as SearchFromAppItemRVAdapter)
                    .filter
                    .filter(it.toString())
            }
        }

        // 获取已安装app
        appItems = runCatchThrowable(catchBlock = { throwable ->
            logInfoAndroid("获取已安装app出错", t = throwable)
            val exceptionStr = throwable.stackTraceToString()
            runOnUiThread {
                UiUtils.createDialog(
                    context = this,
                    titleStr = "错误",
                    text = exceptionStr,
                    enablePositiveBtn = true,
                    positiveBtnText = "确定",
                    enableNegativeBtn = true,
                    negativeBtnText = "复制",
                    negativeBlock = { _: DialogInterface, _: Int ->
                        val clipboardManager = SystemServices.getClipboardManager(this)
                        val clipData = ClipData.newPlainText(
                            BuildConfig.APPLICATION_ID,
                            exceptionStr
                        )
                        clipboardManager.setPrimaryClip(clipData)
                    }
                ).show()
            }
            arrayListOf(PackageUtils.getSelfInfo(this))
        }) {
            PackageUtils.getInstalledPackages(
                context = this,
                filter = null,
            )
        }!!

        // 排序
        sortAppList()

        runOnUiThread {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(this@ShowAllInstalledAppsActivityMaterial3).apply {
                        orientation = LinearLayoutManager.VERTICAL
                    }
                adapter = ShowAllInstalledAppsAdapter3(appItems).apply {
                    isAllowedRefreshInstalledAppsUiProperty = ::isAllowedRefreshInstalledAppsUi
                }.also {
                    appItemsRecyclerViewAdapter = it
                }
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }

            searchHistoryContainer.apply {
                layoutManager =
                    LinearLayoutManager(this@ShowAllInstalledAppsActivityMaterial3).apply {
                        orientation = LinearLayoutManager.VERTICAL
                    }
                adapter = SearchFromAppItemRVAdapter(
                    appItems = appItems,
                    applySearchBlock = { text ->
                        applySearch(text, searchView)
                    }
                )
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 对app进行排序                                                             *
     *                                                                         *
     **************************************************************************/
    // 排序器
    private val appNameComparator = object : Comparator<AppItem> {
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

    private fun sortAppList() {
        // 读取最新的数据
        subProcessOomPolicyMap = prefAll<SubProcessOomPolicy>(
            PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
        )
        appOptimizePolicies = prefAll<AppOptimizePolicy>(
            PreferenceNameConstants.APP_OPTIMIZE_POLICY
        )
        // 开始排序
        val map = HashMap<AppItem, Boolean>(appItems.size)
        appItems.sortWith(
            Comparator.comparing { appItem: AppItem ->
                !hasConfiguredApp(appItem, map)
            }.thenComparator(appNameComparator::compare)
        )
    }

    /**
     * [appItem]是否自定义了配置
     */
    private fun hasConfiguredApp(appItem: AppItem, map: HashMap<AppItem, Boolean>): Boolean {
        map[appItem]?.let { return it }

        val packageName = appItem.packageName
        var result = false

        appItem.appConfiguredEnumSet.clear()
        appOptimizePolicies[packageName]?.let { appOptimizePolicy ->
            if (appOptimizePolicy.disableForegroundTrimMem != null ||
                appOptimizePolicy.disableBackgroundTrimMem != null ||
                appOptimizePolicy.disableBackgroundGc != null
            ) {
                // 正在使用旧版配置
                // 保存新版参数
                ConfigureAppProcessActivityMaterial3.saveAppMemoryOptimize(appOptimizePolicy, this)
            }

            // app是否启用优化
            if (appOptimizePolicy.enableForegroundTrimMem == !PreferenceDefaultValue.enableForegroundTrimMem ||
                appOptimizePolicy.enableBackgroundTrimMem == !PreferenceDefaultValue.enableBackgroundTrimMem ||
                appOptimizePolicy.enableBackgroundGc == !PreferenceDefaultValue.enableBackgroundGc
            ) {
                appItem.appConfiguredEnumSet.add(AppConfiguredEnum.AppOptimizePolicy)
                result = true
            }

            // 自定义oom
            if (appOptimizePolicy.enableCustomMainProcessOomScore) {
                appItem.appConfiguredEnumSet.add(AppConfiguredEnum.CustomMainProcessOomScore)
                result = true
            }

            // 主进程ADJ管理策略
            if (appOptimizePolicy.mainProcessAdjManagePolicy != AppOptimizePolicy.MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_DEFAULT) {
                appItem.appConfiguredEnumSet.add(AppConfiguredEnum.MainProcessAdjManagePolicy)
                result = true
            }
        }

        // 是否配置过子进程oom策略
        var hasConfiguredSubProcessOomPolicy = false
        val appConfiguredEnumIfConfiguredSubProcessOomPolicy = AppConfiguredEnum.SubProcessOomPolicy
        // 在默认白名单
        for (processName in CommonProperties.subProcessDefaultUpgradeSet) {
            if (processName.containsIgnoreCase(packageName)) {
                appItem.appConfiguredEnumSet.add(appConfiguredEnumIfConfiguredSubProcessOomPolicy)
                hasConfiguredSubProcessOomPolicy = true
                break
            }
        }
        if (!hasConfiguredSubProcessOomPolicy) {
            // 从本地配置查找
            for ((subProcessName, subProcessOomPolicy) in subProcessOomPolicyMap.entries) {
                if (subProcessName.containsIgnoreCase(packageName)) {
                    if (subProcessOomPolicy.policyEnum != SubProcessOomPolicyEnum.DEFAULT) {
                        appItem.appConfiguredEnumSet.add(
                            appConfiguredEnumIfConfiguredSubProcessOomPolicy
                        )
                        hasConfiguredSubProcessOomPolicy = true
                        break
                    }
                }
            }
        }
        if (hasConfiguredSubProcessOomPolicy) {
            result = true
        }

        return result.also {
            map[appItem] = it
        }
    }
}