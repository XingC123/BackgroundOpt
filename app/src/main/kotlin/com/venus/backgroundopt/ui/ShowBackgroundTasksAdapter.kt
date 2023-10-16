package com.venus.backgroundopt.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter

/**
 * @author XingC
 * @date 2023/9/23
 */
class ShowBackgroundTasksAdapter(override val items: List<AppItem>) :
    ShowInfoFromAppItemAdapter(items) {
    override fun getText1Content(appItem: AppItem): String? {
        return appItem.processName
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowInfoFromAppItemViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            itemView.findViewById<LinearLayout>(R.id.appItemLastProcessingResultLayout)?.let {
                it.visibility = View.GONE
            }
        }
    }
}