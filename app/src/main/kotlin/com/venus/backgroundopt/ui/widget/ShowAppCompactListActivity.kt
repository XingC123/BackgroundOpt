package com.venus.backgroundopt.ui.widget

import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.widget.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.ui.widget.base.ShowInfoFromAppItemAdapter

/**
 * @author XingC
 * @date 2023/9/25
 */
class ShowAppCompactListActivity : ShowInfoFromAppItemActivity() {
    override fun getShowInfoAdapter(appItems: List<AppItem>): ShowInfoFromAppItemAdapter {
        return GetAppCompactListAdapter(appItems)
    }
}