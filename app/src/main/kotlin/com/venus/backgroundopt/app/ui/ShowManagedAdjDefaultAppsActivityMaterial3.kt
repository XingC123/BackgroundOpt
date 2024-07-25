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

package com.venus.backgroundopt.app.ui

import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.ShowInfoFromAppItemActivityMaterial3
import com.venus.backgroundopt.app.ui.base.sendMessage
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.entity.message.ManagedAdjDefaultAppMessage
import com.venus.backgroundopt.common.util.PackageUtils
import com.venus.backgroundopt.common.util.log.logInfoAndroid
import com.venus.backgroundopt.common.util.message.MessageKeyConstants

/**
 * @author XingC
 * @date 2024/4/23
 */
class ShowManagedAdjDefaultAppsActivityMaterial3 : ShowInfoFromAppItemActivityMaterial3() {
    override fun getShowInfoAdapter(
        appItems: MutableList<AppItem>,
        vararg others: Any?,
    ): ShowManagedAdjDefaultAppsAdapter {
        return ShowManagedAdjDefaultAppsAdapter(this, appItems)
    }

    override fun getRecyclerViewResId(): Int = R.id.recycler_view

    override fun getContentView(): Int = R.layout.activity_show_managed_adj_default_apps_material3

    override fun init() {
        val managedAdjDefaultAppMessage = sendMessage<ManagedAdjDefaultAppMessage>(
            key = MessageKeyConstants.getManagedAdjDefaultApps
        ) ?: return
        val packageNames = managedAdjDefaultAppMessage.defaultAppPackageNames ?: return
        packageNames.forEach {
            logInfoAndroid("packageNames: ${it}")
        }

        val appItems = PackageUtils.getTargetApps(
            context = this,
            packageNames = packageNames
        )

        logInfoAndroid("appItems: ${appItems.size}")

        initRecyclerView(appItems = appItems)
    }
}