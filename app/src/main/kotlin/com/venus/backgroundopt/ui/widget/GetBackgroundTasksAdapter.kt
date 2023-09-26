package com.venus.backgroundopt.ui.widget

import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.widget.base.ShowInfoFromAppItemAdapter

/**
 * @author XingC
 * @date 2023/9/23
 */
class GetBackgroundTasksAdapter(override val items: List<AppItem>) :
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

    override fun getTipText1ResId(): Int {
        return R.string.appItemTipProcessName
    }
}