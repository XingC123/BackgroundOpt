package com.venus.backgroundopt.ui

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivityMaterial3
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.setIntentData

/**
 * @author XingC
 * @date 2023/9/25
 */
class ShowAppCompactListActivityMaterial3 : ShowInfoFromAppItemActivityMaterial3() {
    override fun getShowInfoAdapter(
        appItems: List<AppItem>,
        vararg others: Any?
    ): ShowInfoFromAppItemAdapter {
        return ShowAppCompactListAdapter(appItems)
    }

    override fun getRecyclerViewResId(): Int {
        return R.id.showAppCompactListRecyclerView
    }

    override fun init() {
        val listStr = sendMessage(
            this,
            MessageKeyConstants.getAppCompactList,
        )
        setIntentData(intent, listStr)
        super.init()
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_app_compact_list_material3
    }

    /* *************************************************************************
     *                                                                         *
     * Toolbar                                                                 *
     *                                                                         *
     **************************************************************************/
    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.showAppCompactLisToolBarHelpMenuItem -> {
                UiUtils.createDialog(
                    context = this,
                    viewResId = R.layout.content_show_app_compact_list_toolbar_help
                ).show()
            }
        }
    }
}