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

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.LinearLayoutManager
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.ShowAppCompactListAdapter.ShowAppCompactListViewHolder
import com.venus.backgroundopt.app.ui.base.isKeywordMatched
import com.venus.backgroundopt.app.ui.base.refreshAllItemUi
import com.venus.backgroundopt.app.ui.base.refreshVisibleItemUi
import com.venus.backgroundopt.app.ui.component.NoIndexOutOfBoundsExceptionLinearLayoutManager
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.common.util.unsafeLazy


/**
 * @author XingC
 * @date 2024/7/8
 */
class RunningProcessesAdapter(
    activity: Activity,
    appItems: MutableList<AppItem>,
) : ShowAppCompactListAdapter(activity, appItems), Filterable {
    lateinit var layoutManager: NoIndexOutOfBoundsExceptionLinearLayoutManager

    override fun getViewHolder(view: View): ShowProcessInfoFromAppItemViewHolder =
        RunningProcessesViewHolder(view)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ShowProcessInfoFromAppItemViewHolder {
        val view = doOnCreateViewHolder(parent, viewType)
        return getViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filterAppItems.size
    }

    /* *************************************************************************
     *                                                                         *
     * 进程搜索                                                                 *
     *                                                                         *
     **************************************************************************/
    override fun getFilter(): Filter = appItemSearchFilter

    private val appItemSearchFilter by unsafeLazy {
        object : Filter() {
            // 是否搜索过(控制app展示区内容是否要还原)
            var hasSearched = false
            var lastSearchContent = ""
            var curSearchContent = lastSearchContent
            val filterResult by lazy {
                FilterResults()
            }
            val tmpList = arrayListOf<AppItem>()

            private fun updateFilterAppItems(new: MutableList<AppItem>) {
                appItemsBeforeFilter = new

                filterAppItems()
                sortProcessList()
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchContent = constraint.toString()
                var filterResultValues: List<AppItem>? = null

                if (searchContent.isBlank()) {
                    if (hasSearched) {
                        hasSearched = false
                        curSearchContent = ""
                        updateFilterAppItems(appItems)
                        filterResultValues = filterAppItems
                    }
                } else {
                    val filterList = tmpList.apply { clear() }
                    appItems.forEach { appItem ->
                        if (appItem.isKeywordMatched(searchContent)) {
                            filterList.add(appItem)
                        }
                    }
                    hasSearched = true
                    curSearchContent = searchContent
                    updateFilterAppItems(filterList)
                    filterResultValues = filterAppItems
                }

                return filterResult.apply {
                    values = filterResultValues
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (constraint == lastSearchContent) {
                    refreshVisibleItemUi(activity, layoutManager)
                } else {
                    refreshAllItemUi(activity)
                }
                lastSearchContent = curSearchContent
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 进程排序                                                                  *
     *                                                                         *
     **************************************************************************/
    private lateinit var processListComparator: Comparator<AppItem>

    fun sortProcessListAndRefreshUi() {
        sortProcessList()
        refreshVisibleItemUi(activity, layoutManager)
    }

    fun changeProcessesSort(itemResId: Int) {
        processListComparator = when (itemResId) {
            R.id.runningProcessesUidSortMenuItem -> AppItem.uidComparator
            else -> {
                // 默认按pid
                AppItem.pidComparator
            }
        }
    }

    fun sortProcessList() {
        filterAppItems.sortWith(processListComparator)
    }

    private fun sortProcessListByName() {
        filterAppItems.sortWith { appItem1, appItem2 ->
            AppItem.appNameComparator.compare(appItem2, appItem1)
        }
    }

    private fun sortProcessListByPid() {
        filterAppItems.sortWith { appItem1, appItem2 ->
            AppItem.pidComparator.compare(appItem2, appItem1)
        }
    }

    private fun sortProcessListByUid() {
        filterAppItems.sortWith { appItem1, appItem2 ->
            AppItem.uidComparator.compare(appItem2, appItem1)
        }
    }

    companion object {
        const val PROCESS_SORT_BY_NAME = 1
        const val PROCESS_SORT_BY_PID = 2
        const val PROCESS_SORT_BY_UID = 3
    }
}

class RunningProcessesViewHolder(itemView: View) : ShowAppCompactListViewHolder(itemView)