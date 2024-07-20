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

import android.view.KeyEvent
import android.view.MenuItem
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.SearchFromAppItemRVAdapter
import com.venus.backgroundopt.app.ui.base.ShowInfoFromAppItemActivityMaterial3
import com.venus.backgroundopt.app.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.app.ui.component.NoIndexOutOfBoundsExceptionLinearLayoutManager
import com.venus.backgroundopt.app.ui.component.VenusPopupMenuButton
import com.venus.backgroundopt.app.ui.style.RecycleViewItemSpaceDecoration
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.util.PackageUtils
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.log.logInfoAndroid
import com.venus.backgroundopt.common.util.message.MessageKeyConstants
import com.venus.backgroundopt.common.util.message.sendMessageAcceptList
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.common.util.unsafeLazy
import com.venus.backgroundopt.xposed.entity.self.ProcessRecordBaseInfo
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * @author XingC
 * @date 2024/7/8
 */
class RunningProcessesActivityMaterial3 : ShowInfoFromAppItemActivityMaterial3() {
    private lateinit var processesRV: RecyclerView
    private lateinit var processesRVAdapter: RunningProcessesAdapter
    private lateinit var searchBar: SearchBar
    private lateinit var searchView: SearchView
    private lateinit var searchPredictionRVAdapter: SearchFromAppItemRVAdapter
    private var searchContent: CharSequence = ""

    private val updateRunningProcessExecutor by unsafeLazy {
        ExecutorUtils.newScheduleThreadPool(
            coreSize = 1,
            factoryName = "updateRunningProcessExecutor",
            removeOnCancelPolicy = true
        )
    }
    private var updateRunningProcessesFuture: ScheduledFuture<*>? = null
    private val initialDelay = 3L
    private val delay = 3L
    private val delayTimeUnit = TimeUnit.SECONDS

    override fun onResume() {
        super.onResume()

        startScheduledUpdateAction()
    }

    override fun onStop() {
        super.onStop()

        updateRunningProcessesFuture?.cancel(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        runCatchThrowable {
            updateRunningProcessExecutor.shutdownNow()
        }
    }

    override fun getShowInfoAdapter(
        appItems: MutableList<AppItem>,
        vararg others: Any?,
    ): ShowInfoFromAppItemAdapter {
        return RunningProcessesAdapter(this, appItems)
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.helpMenuItem -> {
                UiUtils.createDialog(
                    context = this,
                    titleResId = R.string.helpStr,
                    textResId = R.string.running_processes_toolbar_help_tip_text
                ).show()
            }

            else -> {}
        }
    }

    override fun getRecyclerViewResId(): Int = R.id.runningProcessesRecycleView

    override fun getContentView(): Int = R.layout.activity_running_processes_material3

    override fun init() {
        // 数据初始化
        appItems = arrayListOf()
        updateRunningProcesses()

        // 布局初始化
        runOnUiThread {
            processesRV = findViewById<RecyclerView>(getRecyclerViewResId()).apply {
                val linearLayoutManager = NoIndexOutOfBoundsExceptionLinearLayoutManager(
                    this@RunningProcessesActivityMaterial3
                ).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                layoutManager = linearLayoutManager
                adapter = RunningProcessesAdapter(
                    this@RunningProcessesActivityMaterial3,
                    appItems
                ).apply {
                    layoutManager = linearLayoutManager
                    enableScrollAnimation = false
                }.also { processesRVAdapter = it }
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }

            // 功能初始化
            initSearchFunction()
            initProcessesSortFunction()
            initProcessesClassifyFunction()
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 搜索                                                                     *
     *                                                                         *
     **************************************************************************/
    private fun initSearchFunction() {
        searchBar = findViewById(R.id.search_bar)
        searchView = findViewById<SearchView>(R.id.runningProcessesSearchView).apply {
            editText.apply {
                setOnEditorActionListener { v, actionId, event ->
                    event?.let { e ->
                        if (e.keyCode == KeyEvent.KEYCODE_ENTER) {
                            searchByKeyword(v.text)
                        }
                    }
                    false
                }

                addTextChangedListener {
                    searchPredictionRVAdapter.filter.filter(it.toString())
                }
            }
        }
        SearchFromAppItemRVAdapter.createSearchPredictionRV(
            activity = this,
            rvResId = R.id.runningProcessesSearchPredictionRecycleView,
            appItems = appItems,
            searchBlock = ::searchByKeyword
        )?.let { rv ->
            searchPredictionRVAdapter = rv.adapter as SearchFromAppItemRVAdapter
        }
    }

    private fun searchByKeyword(keyword: CharSequence) {
        searchBar.setText(keyword)
        searchView.hide()
        filterProcessList(keyword)
        searchContent = keyword
    }

    private fun filterProcessList(keyword: CharSequence) {
        processesRVAdapter.filter.filter(keyword)
    }

    /* *************************************************************************
     *                                                                         *
     * 排序                                                                     *
     *                                                                         *
     **************************************************************************/
    private fun initProcessesSortFunction() {
        val processesSortBtn = findViewById<VenusPopupMenuButton>(R.id.processesSortBtn).apply {
            setExtraOnMenuItemClickListener { menuItem: MenuItem ->
                processesRVAdapter.changeProcessesSort(menuItem.itemId)
                processesRVAdapter.sortProcessListAndRefreshUi()
                true
            }
        }

        // 排序
        processesRVAdapter.changeProcessesSort(processesSortBtn.selectedItemResId)
        processesRVAdapter.sortProcessListAndRefreshUi()
    }

    /* *************************************************************************
     *                                                                         *
     * 分类                                                                     *
     *                                                                         *
     **************************************************************************/
    private fun initProcessesClassifyFunction() {
        val appCategoryBtn = findViewById<VenusPopupMenuButton>(R.id.appCategoryBtn).apply {
            setExtraOnMenuItemClickListener { menuItem: MenuItem ->
                processesRVAdapter.changeCategoryUsedToShowProcesses(menuItem.itemId)
                processesRVAdapter.classifyProcessListAndRefreshUi()
                true
            }
        }

        // 排序
        processesRVAdapter.changeCategoryUsedToShowProcesses(appCategoryBtn.selectedItemResId)
    }

    /* *************************************************************************
     *                                                                         *
     * 列表数据更新                                                              *
     *                                                                         *
     **************************************************************************/
    private val nullProcessList by lazy { arrayListOf<ProcessRecordBaseInfo>() }

    private fun updateRunningProcesses() {
        val newProcessInfos = getRunningProcesses()

        val newAppItems = PackageUtils.getTargetApps(context = this, list = newProcessInfos)
        appItems.clear()
        appItems.addAll(newAppItems)
    }

    private fun updateRunningProcessesAndRefreshUi() {
        updateRunningProcesses()
        // 排序
        processesRVAdapter.sortProcessListAndRefreshUi()
    }

    private fun getRunningProcesses(): MutableList<ProcessRecordBaseInfo> {
        return sendMessageAcceptList<ProcessRecordBaseInfo>(
            context = this,
            key = MessageKeyConstants.RUNNING_PROCESS_LIST
        ) ?: nullProcessList
    }

    private fun startScheduledUpdateAction() {
        updateRunningProcessesFuture = updateRunningProcessExecutor.scheduleWithFixedDelay(
            {
                runCatchThrowable(catchBlock = { throwable: Throwable ->
                    logInfoAndroid("运行中进程列表更新失败", t = throwable)
                }) {
                    // 更新数据
                    updateRunningProcesses()
                    // 是否进行搜索内容过滤
                    if (searchContent.isNotBlank()) {
                        filterProcessList(searchContent)
                    } else {
                        processesRVAdapter.sortProcessListAndRefreshUi()
                    }
                }
            },
            initialDelay,
            delay,
            delayTimeUnit
        )
    }
}