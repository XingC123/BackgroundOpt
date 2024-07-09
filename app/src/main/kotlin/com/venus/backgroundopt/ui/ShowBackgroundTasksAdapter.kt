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
                    
package com.venus.backgroundopt.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemViewHolder
import com.venus.backgroundopt.utils.message.handle.BackgroundTasksMessageHandler

/**
 * @author XingC
 * @date 2023/9/23
 */
class ShowBackgroundTasksAdapter(
    activity: Activity,
    items: MutableList<AppItem>,
    private val backgroundTaskMessage: BackgroundTasksMessageHandler.BackgroundTaskMessage
) : ShowProcessInfoFromAppItemAdapter(activity, items) {
    private var enableForegroundProcTrimMem = false
    private var enableBackgroundProcTrimMem = false

    override fun getText1Content(appItem: AppItem): String {
        return ""
    }

    override fun getText2Content(appItem: AppItem): String {
        return appItem.pid.toString()
    }

    override fun getText3Content(appItem: AppItem): String {
        return appItem.uid.toString()
    }

    override fun getText4Content(appItem: AppItem): String {
        return appItem.curAdj.toString()
    }

    override fun getTipText1ResId(): Int {
        return R.string.appItemTipProcessName
    }

    override fun getViewHolder(view: View): ShowProcessInfoFromAppItemViewHolder {
        return ShowBackgroundTasksViewHolder(view)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowProcessInfoFromAppItemViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            // 上次运行结果
            itemView.findViewById<LinearLayout>(R.id.appItemLastProcessingResultLayout)?.let {
                it.visibility = View.GONE
            }
            // 设置前台可见性
            enableForegroundProcTrimMem =
                PreferenceDefaultValue.isEnableForegroundTrimMem(this.itemView.context)
            // 设置后台可见性
            enableBackgroundProcTrimMem =
                PreferenceDefaultValue.isEnableBackgroundTrimMem(this.itemView.context)
        }
    }

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val appItem = filterAppItems[position]
        val packageName = appItem.packageName
        val viewHolder = holder as ShowBackgroundTasksViewHolder

        // 设置应用名
        viewHolder.appItemTipText1.text = appItem.fullQualifiedProcessName

        var enablePolicyForegroundTrim = enableForegroundProcTrimMem
        var enablePolicyBackgroundTrim = PreferenceDefaultValue.enableBackgroundTrimMem
        var enablePolicyBackgroundGc = PreferenceDefaultValue.enableBackgroundGc

        backgroundTaskMessage.appOptimizePolicyMap[packageName]?.let { appOptimizePolicy ->
            enablePolicyForegroundTrim =
                appOptimizePolicy.enableForegroundTrimMem ?: enablePolicyForegroundTrim
            enablePolicyBackgroundTrim =
                appOptimizePolicy.enableBackgroundTrimMem ?: enablePolicyBackgroundTrim
            enablePolicyBackgroundGc =
                appOptimizePolicy.enableBackgroundGc ?: enablePolicyBackgroundGc
        }

        // 设置提示文字的可见性
        if (!enableForegroundProcTrimMem) {
            setAppOptimizePolicyTextVisible(viewHolder.appItemForegroundTrimMemText, false)
        } else {
            setAppOptimizePolicyTextVisible(
                viewHolder.appItemForegroundTrimMemText,
                enablePolicyForegroundTrim
            )
        }
        if (!enableBackgroundProcTrimMem) {
            setAppOptimizePolicyTextVisible(viewHolder.appItemBackgroundTrimMemText, false)
        } else {
            setAppOptimizePolicyTextVisible(
                viewHolder.appItemBackgroundTrimMemText,
                enablePolicyBackgroundTrim
            )
        }
        setAppOptimizePolicyTextVisible(
            viewHolder.appItemBackgroundGcText,
            enablePolicyBackgroundGc
        )
    }

    private fun setAppOptimizePolicyTextVisible(textView: TextView, isVisible: Boolean) {
        if (isVisible) {
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    class ShowBackgroundTasksViewHolder(itemView: View) : ShowProcessInfoFromAppItemViewHolder(itemView) {
        val appItemTipText1: TextView

        val appItemForegroundTrimMemText: TextView
        val appItemBackgroundTrimMemText: TextView
        val appItemBackgroundGcText: TextView

        init {
            appItemTipText1 = itemView.findViewById(R.id.processName)

            appItemForegroundTrimMemText = itemView.findViewById(R.id.appItemForegroundTrimMemText)
            appItemBackgroundTrimMemText = itemView.findViewById(R.id.appItemBackgroundTrimMemText)
            appItemBackgroundGcText = itemView.findViewById(R.id.appItemBackgroundGcText)
        }
    }
}