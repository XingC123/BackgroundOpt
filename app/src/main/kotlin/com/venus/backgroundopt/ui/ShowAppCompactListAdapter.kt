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

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.manager.process.AbstractAppOptimizeManager.AppOptimizeEnum
import com.venus.backgroundopt.manager.process.ProcessCompactResultCode
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter

/**
 * @author XingC
 * @date 2023/9/25
 */
class ShowAppCompactListAdapter(items: List<AppItem>) : ShowInfoFromAppItemAdapter(items) {
    override fun getText1Content(appItem: AppItem): String {
        return appItem.processName ?: appItem.packageName
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

    override fun getViewHolder(view: View): ShowInfoFromAppItemViewHolder {
        return ShowAppCompactListViewHolder(view)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowInfoFromAppItemViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            itemView.findViewById<LinearLayout>(R.id.appItemAppOptimizePolicyLayout)?.let {
                it.visibility = View.GONE
            }
        }
    }

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val viewHolder = holder as ShowAppCompactListViewHolder
        val view = viewHolder.itemView
        val appItem = items[position]

        // 上一次执行结果
        val textView = viewHolder.appItemLastProcessingResultText
        appItem.lastProcessingResultMap[AppOptimizeEnum.PROCESS_COMPACT]?.let { processingResult ->
            val textResId =
                when (processingResult.lastProcessingCode) {
                    ProcessCompactResultCode.success -> {
                        textView.setTextColor(view.resources.getColor(R.color.green, null))
                        R.string.appItemLastProcessingSuccessResultText
                    }

                    ProcessCompactResultCode.unNecessary -> {
                        textView.setTextColor(view.resources.getColor(R.color.cyan, null))
                        R.string.appItemLastProcessingUnnecessaryResultText
                    }

                    else -> {
                        textView.setTextColor(view.resources.getColor(R.color.shanChui, null))
                        R.string.appItemLastProcessingDoNothingResultText
                    }
                }
            textView.setText(textResId)
        } ?: run {
            textView.text = ""
        }
    }

    class ShowAppCompactListViewHolder(itemView: View) : ShowInfoFromAppItemViewHolder(itemView) {
        var appItemLastProcessingResultText: TextView

        init {
            appItemLastProcessingResultText =
                itemView.findViewById(R.id.appItemLastProcessingResultText)
        }
    }
}