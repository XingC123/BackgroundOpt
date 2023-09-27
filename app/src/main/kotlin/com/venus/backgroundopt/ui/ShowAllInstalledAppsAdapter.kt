package com.venus.backgroundopt.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.utils.TMP_DATA

/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsAdapter(val appItems: List<AppItem>) :
    RecyclerView.Adapter<ShowAllInstalledAppsAdapter.Companion.ShowAllInstalledAppsViewHolder>() {
    companion object {
        class ShowAllInstalledAppsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var appIcon: ImageView
            var appName: TextView

            init {
                appIcon = itemView.findViewById(R.id.installedAppItemAppIcon)
                appName = itemView.findViewById(R.id.installedAppItemAppNameText)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowAllInstalledAppsViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.installed_apps_item, parent, false)
        return ShowAllInstalledAppsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return appItems.size
    }

    override fun onBindViewHolder(holder: ShowAllInstalledAppsViewHolder, position: Int) {
        val appItem = appItems[position]
        holder.appIcon.setImageDrawable(appItem.appIcon)
        holder.appName.text = appItem.appName

        holder.itemView.setOnClickListener { view ->
            view.context.also { context ->
                context.startActivity(
                    Intent(
                        context,
                        ConfigureAppProcessActivity::class.java
                    ).apply {
                        TMP_DATA = appItem
                    })
            }
        }
    }
}