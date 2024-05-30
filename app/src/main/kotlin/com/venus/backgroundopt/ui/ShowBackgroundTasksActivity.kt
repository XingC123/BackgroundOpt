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

import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivity
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.handle.BackgroundTasksMessageHandler.BackgroundTaskMessage
import com.venus.backgroundopt.utils.message.sendMessage


/**
 * 用于展示后台任务列表的Activity
 *
 * @author XingC
 * @date 2023/9/23
 */
@Deprecated(
    message = "默认使用Material3设计风格",
    replaceWith = ReplaceWith("ShowBackgroundTasksActivityMaterial3")
)
class ShowBackgroundTasksActivity : ShowInfoFromAppItemActivity() {
    override fun getShowInfoAdapter(
        appItems: List<AppItem>,
        vararg others: Any?
    ): ShowProcessInfoFromAppItemAdapter {
        return ShowBackgroundTasksAdapter(this, appItems, others[0] as BackgroundTaskMessage)
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.showBackgroundTasksToolBar)
    }

    override fun getRecyclerViewResId(): Int {
        return R.id.showBackgroundTasksRecyclerView
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_background_tasks
    }

    override fun init() {
        val message = sendMessage(
            this,
            MessageKeyConstants.getBackgroundTasks,
        ) ?: return

        val backgroundTaskMessage = JSON.parseObject(message, BackgroundTaskMessage::class.java)
        val appItems = PackageUtils.getTargetApps(this, backgroundTaskMessage.processInfos)
        initRecyclerView(appItems, backgroundTaskMessage)
    }
}