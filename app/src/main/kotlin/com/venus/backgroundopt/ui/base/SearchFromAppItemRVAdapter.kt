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

package com.venus.backgroundopt.ui.base

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.style.RecycleViewItemSpaceDecoration
import com.venus.backgroundopt.utils.containsIgnoreCase
import com.venus.backgroundopt.utils.getView

/**
 * 从List<[AppItem]>中查找匹配给定条件的[AppItem.appName]时可使用本类
 *
 * @author XingC
 * @date 2024/7/8
 */
class SearchFromAppItemRVAdapter(
    val appItems: List<AppItem>,
    val applySearchBlock: (CharSequence) -> Unit,
) : RecyclerView.Adapter<SearchFromAppItemViewHolder>(), Filterable {
    // 匹配到的app名字
    var matchedApp: List<String>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchFromAppItemViewHolder {
        val view = parent.context.getView(
            layoutResId = R.layout.item_search_from_app_item_search_prediction,
            parent = parent
        )
        return SearchFromAppItemViewHolder(view)
    }

    override fun getItemCount(): Int = matchedApp?.size ?: 0

    override fun onBindViewHolder(holder: SearchFromAppItemViewHolder, position: Int) {
        matchedApp ?: return

        val appName = matchedApp!![position]
        // 设置数据
        holder.possibleNameText.text = appName
        // 设置监听
        holder.card.setOnClickListener { _ ->
            applySearchBlock(appName)
        }
    }

    override fun getFilter(): Filter = appItemFilter

    private val appItemFilter by lazy {
        object : Filter() {
            val filterResult by lazy {
                FilterResults()
            }
            val set = mutableSetOf<String>()

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchContent = constraint.toString()

                matchedApp = if (searchContent.isBlank()) {
                    null
                } else {
                    val finalList = ArrayList<String>()
                    appItems.forEach { appItem ->
                        if (appItem.isKeywordMatched(searchContent)) {
                            val appName = appItem.appName
                            if (!set.contains(appName)) {
                                set.add(appName)
                                finalList.add(appName)
                            }
                        }
                    }
                    set.clear()
                    finalList
                }

                return filterResult.apply {
                    values = matchedApp
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        fun createSearchPredictionRV(
            activity: Activity,
            rvResId: Int,
            appItems: MutableList<AppItem>,
            searchBlock: (CharSequence) -> Unit,
        ): RecyclerView? {
            return activity.findViewById<RecyclerView>(rvResId)?.apply {
                layoutManager = LinearLayoutManager(activity).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                adapter = SearchFromAppItemRVAdapter(
                    appItems = appItems,
                    applySearchBlock = searchBlock
                )
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }
        }
    }
}

class SearchFromAppItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val card: MaterialCardView by lazy {
        itemView.findViewById(R.id.itemCard)
    }
    val possibleNameText: TextView by lazy {
        itemView.findViewById(R.id.itemText)
    }
}

/**
 * 给定[keyword]匹配该[AppItem]则返回true
 */
fun AppItem.isKeywordMatched(keyword: String): Boolean {
    return appName.containsIgnoreCase(keyword)
            || packageName.containsIgnoreCase(keyword)
            || fullQualifiedProcessName?.containsIgnoreCase(keyword) == true
}
