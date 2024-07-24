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

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.RecyclerViewAdapter
import com.venus.backgroundopt.app.ui.component.VenusListPreference
import com.venus.backgroundopt.app.utils.getView
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy

/**
 * @author XingC
 * @date 2024/7/24
 */
class ConfigureAppProcessAdapter2(
    val activity: Activity,
    private val processes: List<String>,
    private val subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>,
    private val appItem: AppItem,
) : RecyclerViewAdapter<ConfigureAppProcessVA2>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigureAppProcessVA2 {
        val view = parent.context.getView(
            layoutResId = R.layout.item_configure_app_process2,
            parent = parent,
            attachToRoot = false
        )
        return ConfigureAppProcessVA2(activity, view, subProcessOomPolicyMap)

    }

    override fun getItemCount(): Int = processes.size

    override fun onBindViewHolder(holder: ConfigureAppProcessVA2, position: Int) {
        val processName = processes[position]

        // 进程名称
        holder.preference.title = processName
        holder.processName = processName

        // 显示策略的名字
        val subProcessOomPolicy = subProcessOomPolicyMap[processName]!!
        holder.preference.summary = subProcessOomPolicy.policyEnum.configName

        // 其他
        holder.appItem = appItem
        holder.subProcessOomPolicy = subProcessOomPolicy
    }
}

class ConfigureAppProcessVA2(
    activity: Activity,
    itemView: View,
    private val subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>,
) : RecyclerView.ViewHolder(itemView) {
    val preference: VenusListPreference = itemView.findViewById(
        R.id.appProcessConfigureItem
    )

    lateinit var appItem: AppItem
    lateinit var processName: String
    lateinit var subProcessOomPolicy: SubProcessOomPolicy

    init {
        preference.setOnClickListener {
            ConfigureAppProcessDialogBuilder.createDialog(
                activity,
                preference.summaryTextView,
                processName,
                subProcessOomPolicy,
                subProcessOomPolicyMap,
                appItem
            ).show()
        }
    }
}