package com.venus.backgroundopt.ui

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

    override fun getToolBarTitle(): String {
        return "待压缩进程列表"
    }
}