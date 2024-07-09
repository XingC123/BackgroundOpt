/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
                    
 package com.venus.backgroundopt.ui

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.setIntentData

/**
 * @author XingC
 * @date 2023/9/25
 */
@Deprecated(
    message = "默认使用Material3设计风格",
    replaceWith = ReplaceWith("ShowAppCompactListActivityMaterial3")
)
class ShowAppCompactListActivity : ShowInfoFromAppItemActivity() {
    override fun getShowInfoAdapter(
        appItems: MutableList<AppItem>,
        vararg others: Any?
    ): ShowProcessInfoFromAppItemAdapter {
        return ShowAppCompactListAdapter(this, appItems)
    }

    override fun init() {
        val listStr = sendMessage(
            this,
            MessageKeyConstants.getAppCompactList,
        )
        setIntentData(intent, listStr)
        super.init()
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
                    context = this,
                    viewResId = R.layout.content_show_app_compact_list_toolbar_help
                ).show()
            }
        }
    }
}