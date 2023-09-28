package com.venus.backgroundopt.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy

/**
 * @author XingC
 * @date 2023/9/27
 */
class ConfigureAppProcessAdapter(
    private val processes: List<String>,
    private val subProcessOomPolicyList: List<SubProcessOomPolicy>
) :
    RecyclerView.Adapter<ConfigureAppProcessAdapter.Companion.AppProcessConfigureViewHolder>() {
    companion object {
        class AppProcessConfigureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var processNameTextView: TextView
            var applyConfigNameTextView: TextView

            lateinit var processName: String
            lateinit var subProcessOomPolicy: SubProcessOomPolicy

            init {
                processNameTextView =
                    itemView.findViewById(R.id.appProcessConfigureItemProcessNameText)
                applyConfigNameTextView =
                    itemView.findViewById(R.id.appProcessConfigureItemApplyConfigureNameText)

                // 为策略选择按钮绑定事件
                itemView.findViewById<Button>(R.id.appProcessConfigureItemSelectConfigBtn)
                    ?.setOnClickListener { _ ->
                        ConfigureAppProcessDialogBuilder.createDialog(
                            itemView.context,
                            applyConfigNameTextView,
                            processName,
                            subProcessOomPolicy
                        ).show()
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
        holder.processName = processName
        // 显示策略的名字
        val subProcessOomPolicy = subProcessOomPolicyList[position]
        holder.applyConfigNameTextView.text = subProcessOomPolicy.policyEnum.configName
        holder.subProcessOomPolicy = subProcessOomPolicy
    }
}