package com.venus.backgroundopt.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.manager.process.AppCompactManager.ProcessCompactResultCode
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
        val appItem = items[position]

        appItem.processingResult?.let { processingResult ->
            val textView = viewHolder.appItemLastProcessingResultText
            val view = viewHolder.itemView
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