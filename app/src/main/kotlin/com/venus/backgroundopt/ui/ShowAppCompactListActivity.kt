package com.venus.backgroundopt.ui

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.utils.UiUtils

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

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.showAppCompactLisToolBarHelpMenuItem -> {
                UiUtils.createDialog(
                    this, R.layout.content_show_app_compact_list_toolbar_help
                ).show()
            }
        }
    }
}