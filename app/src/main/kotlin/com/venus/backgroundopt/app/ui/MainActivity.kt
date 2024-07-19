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

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.BaseActivity
import com.venus.backgroundopt.app.ui.widget.QueryInfoDialog
import com.venus.backgroundopt.app.utils.findViewById
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.message.MessageKeyConstants

@Deprecated(
    message = "默认使用Material3设计风格",
    replaceWith = ReplaceWith("MainActivityMaterial3")
)
class MainActivity : BaseActivity(), ILogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
    }

    override fun getContentView(): Int {
        return R.layout.activity_main
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.toolbar)
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.MainActivityAboutMenuItem -> startActivity(
                Intent(
                    this,
                    AboutAppActivity::class.java
                )
            )

            R.id.mainActivitySettingsMenuItem -> startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )

            R.id.mainActivityFeatureMenuItem -> startActivity(
                Intent(
                    this,
                    FeatureActivity::class.java
                )
            )
        }
    }

    private fun init() {
        // app激活状态
        val moduleActive = CommonProperties.isModuleActive()
        if (moduleActive) {
            findViewById<TextView>(R.id.mainActivityModuleActiveText)?.setText(R.string.moduleActive)
        }

        // 查询运行中app的信息
        findViewById<Button>(R.id.getRunningAppInfoBtn, moduleActive)?.setOnClickListener { _ ->
            QueryInfoDialog.createQueryIntDataDialog(this, MessageKeyConstants.getRunningAppInfo)
                .show()
        }

        // 获取应用内存分组
        findViewById<Button>(R.id.getTargetAppGroupBtn, moduleActive)?.setOnClickListener { _ ->
            QueryInfoDialog.createQueryIntDataDialog(this, MessageKeyConstants.getTargetAppGroup)
                .show()
        }

        // 获取后台任务列表
        findViewById<Button>(R.id.getBackgroundTasksBtn, moduleActive)?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowBackgroundTasksActivity::class.java))
        }

        // 获取后台内存压缩任务列表
        findViewById<Button>(R.id.getAppCompactItemsBtn, moduleActive)?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowAppCompactListActivity::class.java))
        }

        // 转去设置应用进程页面
        findViewById<Button>(
            R.id.gotoConfigureAppProcessActivityBtn, moduleActive
        )?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowAllInstalledAppsActivity::class.java))
        }
    }
}