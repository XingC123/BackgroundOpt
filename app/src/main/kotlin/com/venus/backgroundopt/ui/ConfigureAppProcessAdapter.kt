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
    RecyclerView.Adapter<ConfigureAppProcessAdapter.ConfigureAppProcessViewHolder>() {
    class ConfigureAppProcessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val processNameTextView: TextView by lazy {
            itemView.findViewById(R.id.appProcessConfigureItemProcessNameText)
        }
        val applyConfigNameTextView: TextView by lazy {
            itemView.findViewById(R.id.appProcessConfigureItemApplyConfigureNameText)
        }

        lateinit var processName: String
        lateinit var subProcessOomPolicy: SubProcessOomPolicy

        init {
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConfigureAppProcessViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_configure_app_process, parent, false)
        return ConfigureAppProcessViewHolder(view)
    }

    override fun getItemCount(): Int {
        return processes.size
    }

    override fun onBindViewHolder(holder: ConfigureAppProcessViewHolder, position: Int) {
        val processName = processes[position]
        holder.processNameTextView.text = processName
        holder.processName = processName
        // 显示策略的名字
        val subProcessOomPolicy = subProcessOomPolicyList[position]
        holder.applyConfigNameTextView.text = subProcessOomPolicy.policyEnum.configName
        holder.subProcessOomPolicy = subProcessOomPolicy
    }
}