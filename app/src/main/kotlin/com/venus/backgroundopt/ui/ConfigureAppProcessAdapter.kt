package com.venus.backgroundopt.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R

/**
 * @author XingC
 * @date 2023/9/27
 */
class ConfigureAppProcessAdapter(val processes: List<String>) :
    RecyclerView.Adapter<ConfigureAppProcessAdapter.Companion.AppProcessConfigureViewHolder>() {
    companion object {
        class AppProcessConfigureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var processNameTextView: TextView
            var applyConfigNameTextView: TextView

            init {
                processNameTextView =
                    itemView.findViewById(R.id.appProcessConfigureItemProcessNameText)
                applyConfigNameTextView =
                    itemView.findViewById(R.id.appProcessConfigureItemApplyConfigureNameText)

                itemView.findViewById<Button>(R.id.appProcessConfigureItemSelectConfigBtn)
                    ?.setOnClickListener { _ ->
                        // TODO 弹出框选择配置: 1. 默认    2. 主进程  3. 不作为(观望)
                    }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppProcessConfigureViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.configure_app_process_item, parent, false)
        return AppProcessConfigureViewHolder(view)
    }

    override fun getItemCount(): Int {
        return processes.size
    }

    override fun onBindViewHolder(holder: AppProcessConfigureViewHolder, position: Int) {
        val processName = processes[position]
        holder.processNameTextView.text = processName
        // TODO 根据进程名查找存储在本地的配置
    }
}