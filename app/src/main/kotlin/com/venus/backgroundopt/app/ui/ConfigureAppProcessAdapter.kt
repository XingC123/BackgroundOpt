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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy

/**
 * @author XingC
 * @date 2023/9/27
 */
class ConfigureAppProcessAdapter(
    private val appItem: AppItem,
    private val processes: List<String>,
    private val subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>
) :
    RecyclerView.Adapter<ConfigureAppProcessAdapter.ConfigureAppProcessViewHolder>() {
    class ConfigureAppProcessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val processNameTextView: TextView by lazy {
            itemView.findViewById(R.id.appProcessConfigureItemProcessNameText)
        }
        val applyConfigNameTextView: TextView by lazy {
            itemView.findViewById(R.id.appProcessConfigureItemApplyConfigureNameText)
        }

        lateinit var appItem: AppItem
        lateinit var processName: String
        lateinit var subProcessOomPolicy: SubProcessOomPolicy
        lateinit var subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>

        init {
            itemView.findViewById<Button>(
                R.id.appProcessConfigureItemSelectConfigBtn
            )?.setOnClickListener { _ ->
                    ConfigureAppProcessDialogBuilder.createDialog(
                        itemView.context,
                        applyConfigNameTextView,
                        processName,
                        subProcessOomPolicy,
                        subProcessOomPolicyMap,
                        appItem
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
        val subProcessOomPolicy = subProcessOomPolicyMap[processName]!!
        holder.applyConfigNameTextView.text = subProcessOomPolicy.policyEnum.configName
        holder.subProcessOomPolicy = subProcessOomPolicy
        holder.subProcessOomPolicyMap = subProcessOomPolicyMap
        holder.appItem = appItem
    }
}