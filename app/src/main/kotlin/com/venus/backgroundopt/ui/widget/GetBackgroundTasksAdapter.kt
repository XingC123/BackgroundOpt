package com.venus.backgroundopt.ui.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord

/**
 * @author XingC
 * @date 2023/9/23
 */
class GetBackgroundTasksAdapter(private val items: List<AppItem>) :
    RecyclerView.Adapter<GetBackgroundTasksAdapter.Companion.GetBackgroundTasksViewHolder>() {
    companion object {
        class GetBackgroundTasksViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var appIcon: ImageView
            var getBackgroundTasksText1: TextView
            var getBackgroundTasksText2: TextView

            init {
                appIcon = itemView.findViewById(R.id.getBackgroundTasksAppIcon)
                getBackgroundTasksText1 = itemView.findViewById(R.id.getBackgroundTasksText1)
                getBackgroundTasksText2 = itemView.findViewById(R.id.getBackgroundTasksText2)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GetBackgroundTasksViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.get_background_tasks_recycler_item, parent, false)
        // 修改tip内容
        view.findViewById<TextView>(R.id.getBackgroundTasksTipText1)
            ?.setText(R.string.backgroundTasksTipText1)
        view.findViewById<TextView>(R.id.getBackgroundTasksTipText2)
            ?.setText(R.string.backgroundTasksTipText2)
        return GetBackgroundTasksViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: GetBackgroundTasksViewHolder, position: Int) {
        val appItem = items[position]
        holder.appIcon.setImageDrawable(appItem.appIcon)
        holder.getBackgroundTasksText1.text = appItem.packageName
        holder.getBackgroundTasksText2.text = appItem.pid.toString()
    }
}