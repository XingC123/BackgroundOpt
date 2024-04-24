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

import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivityMaterial3
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
class ShowBackgroundTasksActivityMaterial3 : ShowInfoFromAppItemActivityMaterial3() {
    override fun getShowInfoAdapter(
        appItems: List<AppItem>,
        vararg others: Any?
    ): ShowProcessInfoFromAppItemAdapter {
        return ShowBackgroundTasksAdapter(appItems, others[0] as BackgroundTaskMessage)
    }

    override fun getRecyclerViewResId(): Int {
        return R.id.showBackgroundTasksRecyclerView
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_background_tasks_material3
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