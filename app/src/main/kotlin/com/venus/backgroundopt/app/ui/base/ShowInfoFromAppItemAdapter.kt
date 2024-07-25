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

package com.venus.backgroundopt.app.ui.base

import android.app.Activity
import com.venus.backgroundopt.R
import com.venus.backgroundopt.common.entity.AppItem

/**
 * 从[AppItem]中展示信息的Adapter都应继承自此类
 *
 * @author XingC
 * @date 2024/4/24
 */
abstract class ShowInfoFromAppItemAdapter(
    protected val activity: Activity,
    protected val appItems: MutableList<AppItem>,
) : RecyclerViewAdapter<ShowInfoFromAppItemViewHolder>() {
    /**
     * 过滤后的列表。
     *
     * 可用于排序、筛选、分类等
     */
    var filterAppItems: MutableList<AppItem> = appItems

    /* *************************************************************************
     *                                                                         *
     * 分类                                                                     *
     *                                                                         *
     **************************************************************************/
    protected var appItemsBeforeFilter = filterAppItems
    protected lateinit var methodFilterAppItems: () -> Unit

    fun changeCategoryToShowAppItems(resId: Int) {
        methodFilterAppItems = when (resId) {
            R.id.runningProcessesUserAppCategoryMenuItem -> { -> filterAppItemsWithUserCategory() }
            R.id.runningProcessesSystemAppCategoryMenuItem -> { -> filterAppItemsBySystemCategory() }
            else -> { -> filterAppItemsByAllCategory() }
        }
    }

    fun filterAppItems() {
        methodFilterAppItems()
    }

    /**
     * 对列表进行分类并刷新显示
     */
    fun filterAppItemsAndRefreshUi() {
        filterAppItems()
        refreshAllItemUi(activity)
    }

    private fun filterAppItemsByAllCategory() {
        filterAppItems = appItemsBeforeFilter
    }

    private fun filterAppItemsWithUserCategory() {
        filterAppItems = appItemsBeforeFilter.filter { appItem ->
            !appItem.systemApp
        }.toMutableList()
    }

    private fun filterAppItemsBySystemCategory() {
        filterAppItems = appItemsBeforeFilter.filter { appItem ->
            appItem.systemApp
        }.toMutableList()
    }
}