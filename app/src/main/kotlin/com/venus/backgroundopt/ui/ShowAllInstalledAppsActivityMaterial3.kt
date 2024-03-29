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

package com.venus.backgroundopt.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.ui.base.BaseActivityMaterial3
import com.venus.backgroundopt.ui.style.RecycleViewItemSpaceDecoration
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.utils.preference.prefBoolean
import com.venus.backgroundopt.utils.preference.prefString
import com.venus.backgroundopt.utils.showProgressBarViewForAction


/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsActivityMaterial3 : BaseActivityMaterial3() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction("正在获取已安装应用...") {
            init()
        }
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_all_installed_apps_material3
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.installerAppToolbarHelpMenuItem -> {
                UiUtils.createDialog(this, R.layout.content_installed_app_toolbar_help).show()
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
            appNameSearchBar.text = text
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
                (searchHistoryContainer.adapter as ShowAllInstalledAppsSearchHistoryRecycleViewAdapter)
                    .filter
                    .filter(it.toString())
            }
        }

        /*val appItems = PackageUtils.getInstalledPackages(this) { packageInfo ->
            !ActivityManagerService.isImportantSystemApp(packageInfo.applicationInfo)
                    || PackageUtils.isHasActivity(packageInfo)
        }*/
        val isEnabledGlobalOomScore = prefBoolean(
            name = PreferenceNameConstants.MAIN_SETTINGS,
            key = PreferenceKeyConstants.GLOBAL_OOM_SCORE,
        )
        val globalOomScoreEffectiveScopeEnum = try {
            GlobalOomScoreEffectiveScopeEnum.valueOf(
                prefString(
                    name = PreferenceNameConstants.MAIN_SETTINGS,
                    key = PreferenceKeyConstants.GLOBAL_OOM_SCORE_EFFECTIVE_SCOPE,
                    defaultValue = PreferenceDefaultValue.globalOomScoreEffectiveScopeName
                )!!
            )
        } catch (t: Throwable) {
            null
        }
        val appItems = PackageUtils.getInstalledPackages(
            context = this,
            filter = null,
        )
        runOnUiThread {
            recyclerView.apply {
                layoutManager =
                    LinearLayoutManager(this@ShowAllInstalledAppsActivityMaterial3).apply {
                        orientation = LinearLayoutManager.VERTICAL
                    }
                adapter = ShowAllInstalledAppsAdapter3(appItems)
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }

            searchHistoryContainer.apply {
                layoutManager =
                    LinearLayoutManager(this@ShowAllInstalledAppsActivityMaterial3).apply {
                        orientation = LinearLayoutManager.VERTICAL
                    }
                adapter = ShowAllInstalledAppsSearchHistoryRecycleViewAdapter(
                    items = appItems,
                    applySearchBlock = { text ->
                        applySearch(text, searchView)
                    }
                )
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }
        }
    }
}