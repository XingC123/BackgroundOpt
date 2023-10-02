package com.venus.backgroundopt.ui

import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter

/**
 * @author XingC
 * @date 2023/9/25
 */
class ShowAppCompactListActivity : ShowInfoFromAppItemActivity() {
    override fun getShowInfoAdapter(appItems: List<AppItem>): ShowInfoFromAppItemAdapter {
        return ShowAppCompactListAdapter(appItems)
    }

    override fun getRecyclerViewResId(): Int {
        return R.id.showAppCompactListRecyclerView
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.showBackgroundTasksToolBar)
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_app_compact_list
    }
}