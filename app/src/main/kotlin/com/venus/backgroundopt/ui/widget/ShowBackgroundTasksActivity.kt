package com.venus.backgroundopt.ui.widget

import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.widget.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.ui.widget.base.ShowInfoFromAppItemAdapter


/**
 * 用于展示后台任务列表的Activity
 *
 * @author XingC
 * @date 2023/9/23
 */
class ShowBackgroundTasksActivity : ShowInfoFromAppItemActivity() {
    override fun getLayoutResId(): Int {
        return R.layout.get_background_tasks_recycler_view
    }

    override fun getShowInfoAdapter(appItems: List<AppItem>): ShowInfoFromAppItemAdapter {
        return GetBackgroundTasksAdapter(appItems)
    }
}