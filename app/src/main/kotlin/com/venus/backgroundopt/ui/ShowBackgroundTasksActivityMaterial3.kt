package com.venus.backgroundopt.ui

import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemActivityMaterial3
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter
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
    ): ShowInfoFromAppItemAdapter {
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