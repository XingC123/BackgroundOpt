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
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.app.ui.base.ShowInfoFromAppItemViewHolder
import com.venus.backgroundopt.app.ui.component.NoIndexOutOfBoundsExceptionLinearLayoutManager
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.app.utils.setTmpData
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.util.unsafeLazy
import kotlin.math.max
import kotlin.reflect.KMutableProperty0

/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsAdapter3(
    activity: Activity,
    appItems: MutableList<AppItem>,
) : ShowInfoFromAppItemAdapter(activity, appItems), Filterable {
    /* *************************************************************************
     *                                                                         *
     * 已安装app列表的刷新                                                        *
     *                                                                         *
     **************************************************************************/
    lateinit var isAllowedRefreshInstalledAppsUiProperty: KMutableProperty0<Boolean>

    /*
     * 辅助列表刷新
     * 用以在点击appItem进入配置页又返回后, 界面中item位置刷新时控制刷新元素个数
     */
    // 该appItem进入配置页时的索引
    private var indexBeforeConfiguredAppItem = appItems.size - 1

    // 进入配置页的appItem
    private var lastConfiguredAppItem: AppItem = appItems.last()

    private val appItemListNeedRefreshItemCount: Int
        get() {
            val index = appItems.indexOf(lastConfiguredAppItem)
            return max(index, indexBeforeConfiguredAppItem) + 1
        }

    /**
     * 更新 刷新[AppItem]组成的[RecyclerView]的items所需要的参数
     * @param index Int 进入配置页前的[AppItem]的索引
     * @param appItem AppItem 进入配置页的[AppItem]
     */
    private fun updateAppItemListRefreshParam(index: Int, appItem: AppItem) {
        indexBeforeConfiguredAppItem = index
        lastConfiguredAppItem = appItem

        isAllowedRefreshInstalledAppsUiProperty.set(true)
    }

    /**
     * 刷新已安装app列表的ui界面
     */
    fun refreshInstalledAppsListUI() {
        notifyItemRangeChanged(0, appItemListNeedRefreshItemCount, 1 /* 防止列表刷新时闪烁 */)
    }

    /* *************************************************************************
     *                                                                         *
     * ui显示                                                                   *
     *                                                                         *
     **************************************************************************/
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ShowAllInstalledAppsViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_installed_apps3, parent, false)
        return ShowAllInstalledAppsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filterAppItems.size
    }

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        holder as ShowAllInstalledAppsViewHolder

        val appItem = filterAppItems[position]
        holder.appIcon.setImageDrawable(appItem.appIcon)
        holder.appName.text = appItem.appName

        setAppFlagTextVisible(
            holder.itemInstalledAppsMemTrimFlagText,
            appItem,
            AppItem.AppConfiguredEnum.AppOptimizePolicy
        )
        setAppFlagTextVisible(
            holder.itemInstalledAppsCustomMainProcOomScoreFlagText,
            appItem,
            AppItem.AppConfiguredEnum.CustomMainProcessOomScore
        )
        setAppFlagTextVisible(
            holder.itemInstalledAppsOomPolicyFlagText,
            appItem,
            AppItem.AppConfiguredEnum.SubProcessOomPolicy
        )
        setAppFlagTextVisible(
            holder.itemInstalledAppsShouldHandleMainProcAdjFlagText,
            appItem,
            AppItem.AppConfiguredEnum.ShouldHandleMainProcAdj
        )
        setAppFlagTextVisible(
            holder.itemInstalledAppsMainProcessAdjManagePolicy,
            appItem,
            AppItem.AppConfiguredEnum.MainProcessAdjManagePolicy
        )

        holder.itemView.setOnClickListener { view ->
            view.context.also { context ->
                context.startActivity(
                    Intent(
                        context,
                        ConfigureAppProcessActivityMaterial3::class.java
                    ).apply {
                        setTmpData(appItem)
                    }
                )
                updateAppItemListRefreshParam(
                    index = position,
                    appItem = appItem
                )
            }
        }
    }

    // 设置显示已配置的设置的标识
    private fun setAppFlagTextVisible(
        component: View,
        appItem: AppItem,
        appConfiguredEnum: AppItem.AppConfiguredEnum,
    ) {
        UiUtils.setComponentVisible(
            component,
            appItem.appConfiguredEnumSet.contains(appConfiguredEnum)
        )
    }

    /* *************************************************************************
     *                                                                         *
     * 根据搜索内容过滤当前列表                                                     *
     *                                                                         *
     **************************************************************************/
    override fun getFilter(): Filter {
        return appItemFilter
    }

    private val appItemFilter by lazy {
        object : Filter() {
            // 是否搜索过(控制app展示区内容是否要还原)
            var hasSearched = false
            var lastSearchAppName = ""
            val filterResult by lazy {
                FilterResults()
            }
            val tmpList = arrayListOf<AppItem>()

            private fun updateFilterAppItems(new: MutableList<AppItem>) {
                appItemsBeforeFilter = new

                filterAppItems()
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchAppName = constraint.toString()

                if (searchAppName.isEmpty()) {
                    if (hasSearched) {
                        hasSearched = false
                        lastSearchAppName = ""
                        updateFilterAppItems(appItems)
                        return filterResult.apply {
                            values = filterAppItems
                        }
                    }
                } else if (searchAppName != lastSearchAppName) {
                    val filterList = tmpList.apply { clear() }
                    appItems.forEach { appItem ->
                        if (appItem.appName.contains(searchAppName, ignoreCase = true)) {
                            filterList.add(appItem)
                        }
                    }
                    updateFilterAppItems(filterList)
                    hasSearched = true
                    lastSearchAppName = searchAppName

                    return filterResult.apply {
                        values = filterAppItems
                    }
                }
                return filterResult.apply { values = null }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }
}

class ShowAllInstalledAppsViewHolder(itemView: View) : ShowInfoFromAppItemViewHolder(itemView) {
    val appIcon: ImageView by unsafeLazy { itemView.findViewById(R.id.appIconImageView) }
    val appName: TextView by unsafeLazy { itemView.findViewById(R.id.appNameText) }
    val itemInstalledAppsMemTrimFlagText: TextView by unsafeLazy {
        itemView.findViewById(R.id.itemInstalledAppsMemTrimFlagText)
    }
    val itemInstalledAppsCustomMainProcOomScoreFlagText: TextView by unsafeLazy {
        itemView.findViewById(R.id.itemInstalledAppsCustomMainProcOomScoreFlagText)
    }
    val itemInstalledAppsOomPolicyFlagText: TextView by unsafeLazy {
        itemView.findViewById(R.id.itemInstalledAppsOomPolicyFlagText)
    }
    val itemInstalledAppsShouldHandleMainProcAdjFlagText: TextView by unsafeLazy {
        itemView.findViewById(R.id.itemInstalledAppsShouldHandleMainProcAdjFlagText)
    }
    val itemInstalledAppsMainProcessAdjManagePolicy: TextView by unsafeLazy {
        itemView.findViewById(R.id.itemInstalledAppsMainProcessAdjManagePolicy)
    }
}