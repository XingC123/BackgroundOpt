package com.venus.backgroundopt.ui

import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter


/**
 * 用于展示后台任务列表的Activity
 *
 * @author XingC
 * @date 2023/9/23
 */
class ShowBackgroundTasksActivity : ShowInfoFromAppItemActivity() {
    override fun getShowInfoAdapter(appItems: List<AppItem>): ShowInfoFromAppItemAdapter {
        return ShowBackgroundTasksAdapter(appItems)
    }

    override fun getToolBarTitle(): String {
        return "后台任务列表"
    }
}