package com.venus.backgroundopt.ui

import android.annotation.SuppressLint
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
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.utils.StringUtils
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.setTmpData

/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsAdapter3(
    private val appItems: List<AppItem>
) : RecyclerView.Adapter<ShowAllInstalledAppsAdapter3.ShowAllInstalledAppsViewHolder>(),
    Filterable {
    class ShowAllInstalledAppsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var appIcon: ImageView
        var appName: TextView
        var itemInstalledAppsMemTrimFlagText: TextView
        var itemInstalledAppsCustomMainProcOomScoreFlagText: TextView
        var itemInstalledAppsOomPolicyFlagText: TextView

        var hadSetClickedListener: Boolean = false

        init {
            appIcon = itemView.findViewById(R.id.installedAppItemAppIcon)
            appName = itemView.findViewById(R.id.installedAppItemAppNameText)
            itemInstalledAppsMemTrimFlagText =
                itemView.findViewById(R.id.itemInstalledAppsMemTrimFlagText)
            itemInstalledAppsCustomMainProcOomScoreFlagText =
                itemView.findViewById(R.id.itemInstalledAppsCustomMainProcOomScoreFlagText)
            itemInstalledAppsOomPolicyFlagText =
                itemView.findViewById(R.id.itemInstalledAppsOomPolicyFlagText)
        }
    }

    /* *************************************************************************
     *                                                                         *
     * ui显示                                                                   *
     *                                                                         *
     **************************************************************************/
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowAllInstalledAppsViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_installed_apps3, parent, false)
        return ShowAllInstalledAppsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filterAppItems.size
    }

    override fun onBindViewHolder(holder: ShowAllInstalledAppsViewHolder, position: Int) {
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

        if (!holder.hadSetClickedListener) {
            holder.itemView.setOnClickListener { view ->
                view.context.also { context ->
                    context.startActivity(
                        Intent(
                            context,
                            ConfigureAppProcessActivityMaterial3::class.java
                        ).apply {
                            setTmpData(appItem)
                        })
                }
            }
        }
    }

    // 设置显示已配置的设置的标识
    private fun setAppFlagTextVisible(
        component: View,
        appItem: AppItem,
        appConfiguredEnum: AppItem.AppConfiguredEnum
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
    // 过滤后的列表。用于搜索功能
    private var filterAppItems: List<AppItem> = appItems

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

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchAppName = constraint.toString()

                if (StringUtils.isEmpty(searchAppName)) {
                    if (hasSearched) {
                        hasSearched = false
                        lastSearchAppName = ""
                        filterAppItems = appItems
                        return filterResult.apply {
                            values = filterAppItems
                        }
                    }
                } else if (searchAppName != lastSearchAppName) {
                    val filterList = arrayListOf<AppItem>()
                    appItems.forEach { appItem ->
                        if (appItem.appName.contains(searchAppName, ignoreCase = true)) {
                            filterList.add(appItem)
                        }
                    }
                    filterAppItems = filterList
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