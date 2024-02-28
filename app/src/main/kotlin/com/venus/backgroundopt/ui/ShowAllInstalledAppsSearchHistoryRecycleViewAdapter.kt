package com.venus.backgroundopt.ui

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.utils.getView


/**
 * @author XingC
 * @date 2024/2/22
 */
class ShowAllInstalledAppsSearchHistoryRecycleViewAdapter(
    val items: List<AppItem>,
    val applySearchBlock: (CharSequence) -> Unit
) : RecyclerView.Adapter<ShowAllInstalledAppsSearchHistoryRecycleViewViewHolder>(), Filterable {
    // 匹配到的app名字
    var matchedApp: List<String>? = null
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowAllInstalledAppsSearchHistoryRecycleViewViewHolder {
        val view = parent.context.getView(
            layoutResId = R.layout.item_show_all_installed_apps_search_history,
            parent = parent
        )
        return ShowAllInstalledAppsSearchHistoryRecycleViewViewHolder(view)
    }

    override fun getItemCount(): Int = matchedApp?.size ?: 0

    override fun onBindViewHolder(
        holder: ShowAllInstalledAppsSearchHistoryRecycleViewViewHolder,
        position: Int
    ) {
        matchedApp ?: return

        val appName = matchedApp!![position]
        // 设置数据
        holder.possibleNameText.text = appName
        // 设置监听
        holder.card.setOnClickListener { _ ->
            applySearchBlock(appName)
        }
    }

    override fun getFilter(): Filter = appNameFilter

    private val appNameFilter by lazy {
        object : Filter() {
            val filterResult by lazy {
                FilterResults()
            }

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                matchedApp = if (constraint.isNullOrBlank()) {
                    null
                } else {
                    val finalList = ArrayList<String>()
                    items.forEach { appItem ->
                        if (appItem.appName.contains(constraint, ignoreCase = true)) {
                            finalList.add(appItem.appName)
                        }
                    }
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
}

class ShowAllInstalledAppsSearchHistoryRecycleViewViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    val card: MaterialCardView by lazy {
        itemView.findViewById(R.id.showAllInstalledAppsSearchHistoryItemCard)
    }
    val possibleNameText: TextView by lazy {
        itemView.findViewById(R.id.showAllInstalledAppsSearchHistoryItemText)
    }
}