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
import android.text.InputType
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.BaseActivityMaterial3
import com.venus.backgroundopt.app.ui.base.ifVersionIsCompatible
import com.venus.backgroundopt.app.ui.widget.QueryInfoDialog
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.app.utils.findViewById
import com.venus.backgroundopt.app.utils.setTmpData
import com.venus.backgroundopt.app.utils.showProgressBarViewForAction
import com.venus.backgroundopt.common.entity.message.HomePageModuleInfoMessage
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.util.PackageUtils
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.message.IMessageSender
import com.venus.backgroundopt.common.util.message.MessageKeyConstants
import com.venus.backgroundopt.common.util.message.messageSender


class MainActivityMaterial3 : BaseActivityMaterial3(), ILogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
    }

    override fun getContentView(): Int {
        return R.layout.activity_main_material3
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.MainActivityAboutMenuItem -> startActivity(
                Intent(
                    this,
                    AboutAppActivityMaterial3::class.java
                )
            )

            R.id.mainActivitySettingsMenuItem -> startActivity(
                Intent(
                    this,
                    SettingsActivityMaterial3::class.java
                )
            )

            R.id.mainActivityFeatureMenuItem -> startActivity(
                Intent(
                    this,
                    FeatureActivityMaterial3::class.java
                )
            )
        }
    }

    private fun init() {
        // app激活状态
        val moduleActive = CommonProperties.isModuleActive()
        if (moduleActive) {
            findViewById<TextView>(R.id.mainActivityModuleActiveText)?.setText(R.string.moduleActive)
            // 获取要展示的信息
            var socketPort: Int? = null
            val socketPortText: TextView = findViewById(R.id.socket_port)

            IMessageSender.sendDefault<HomePageModuleInfoMessage>(
                context = this,
                key = MessageKeyConstants.getHomePageModuleInfo,
            )?.let { homePageModuleInfoMessage ->
                findViewById<TextView>(R.id.max_adj_score_text)?.text =
                    homePageModuleInfoMessage.defaultMaxAdjStr
                findViewById<TextView>(R.id.trim_mem_opt_threshold)?.text =
                    homePageModuleInfoMessage.minOptimizeRssInMBytesStr
                socketPort = homePageModuleInfoMessage.socketPort
            }
            messageSender.init(
                context = this,
                socketPort = socketPort,
                socketPortText = socketPortText
            )
        }

        // 查询运行中app的信息
        findViewById<Button>(R.id.getRunningAppInfoBtn, moduleActive)?.setOnClickListener { _ ->
            QueryInfoDialog.createQueryIntDataDialog(this, MessageKeyConstants.getRunningAppInfo)
                .show()
        }

        // 运行中的进程
        findViewById<Button>(R.id.gotoRunningProcessesBtn, moduleActive)?.setOnClickListener { _ ->
            ifVersionIsCompatible(
                targetVersionCode = 206,
                isForcible = true,
                isNeedModuleRunning = true
            ) {
                startActivity(Intent(this, RunningProcessesActivityMaterial3::class.java))
            }
        }

        // 获取应用内存分组
        findViewById<Button>(R.id.getTargetAppGroupBtn, moduleActive)?.setOnClickListener { _ ->
            QueryInfoDialog.createQueryIntDataDialog(this, MessageKeyConstants.getTargetAppGroup)
                .show()
        }

        // 获取后台任务列表
        findViewById<Button>(R.id.getBackgroundTasksBtn, moduleActive)?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowBackgroundTasksActivityMaterial3::class.java))
        }

        // 获取后台内存压缩任务列表
        findViewById<Button>(R.id.getAppCompactItemsBtn, moduleActive)?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowAppCompactListActivityMaterial3::class.java))
        }

        // 被管理adj的默认app
        findViewById<Button>(
            resId = R.id.getManageAdjDefalutAppsBtn,
            enable = moduleActive
        )?.setOnClickListener { _ ->
            ifVersionIsCompatible(targetVersionCode = 201) {
                runOnUiThread {
                    startActivity(
                        Intent(
                            this,
                            ShowManagedAdjDefaultAppsActivityMaterial3::class.java
                        )
                    )
                }
            }
        }

        /*
         * 转去设置应用进程页面
         */
        val versionForConfiguringApp = 204
        val isForcibleForConfiguringApp = false
        val isNeedModuleRunningForConfiguringApp = true

        findViewById<Button>(
            R.id.gotoConfigureAppProcessActivityBtn, moduleActive
        )?.setOnClickListener { _ ->
            ifVersionIsCompatible(
                targetVersionCode = versionForConfiguringApp,
                isForcible = isForcibleForConfiguringApp,
                isNeedModuleRunning = isNeedModuleRunningForConfiguringApp
            ) {
                runOnUiThread {
                    startActivity(Intent(this, ShowAllInstalledAppsActivityMaterial3::class.java))
                }
            }
        }

        findViewById<Button>(
            R.id.configureAppByPkgNameBtn, moduleActive
        )?.setOnClickListener { _ ->
            ifVersionIsCompatible(
                targetVersionCode = versionForConfiguringApp,
                isForcible = isForcibleForConfiguringApp,
                isNeedModuleRunning = isNeedModuleRunningForConfiguringApp
            ) {
                runOnUiThread {
                    UiUtils.createDialog(
                        context = this,
                        titleResId = R.string.configure_app_by_pkg_name_btn,
                        viewResId = R.layout.query_info,
                        viewBlock = {
                            val editText = findViewById<TextInputEditText>(R.id.queryInfoEditText).apply {
                                // 提示文本
                                setHint(R.string.package_name)
                            }
                            // 设置按钮点击事件
                            findViewById<Button>(R.id.doQueryBtn)?.setOnClickListener doQueryBtn@{ _ ->
                                val appItem = PackageUtils.getAppItemForConfiguration(
                                    packageName = editText.text?.trim().toString(),
                                    packageManager = packageManager
                                ) ?: run {
                                    findViewById<TextView>(R.id.queryResultText).setText(R.string.app_not_exist_tip)
                                    return@doQueryBtn
                                }

                                showProgressBarViewForAction {
                                    setTmpData(appItem)
                                    startActivity(
                                        Intent(
                                            this@MainActivityMaterial3,
                                            ConfigureAppProcessActivityMaterial3::class.java
                                        )
                                    )
                                }
                            }
                        }
                    ).show()
                }
            }
        }
    }
}