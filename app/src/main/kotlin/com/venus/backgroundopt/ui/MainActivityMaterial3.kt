package com.venus.backgroundopt.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.ui.base.BaseActivityMaterial3
import com.venus.backgroundopt.ui.widget.QueryInfoDialog
import com.venus.backgroundopt.utils.findViewById
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.message.MessageKeyConstants


class MainActivityMaterial3 : BaseActivityMaterial3(), ILogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
    }

    override fun getContentView(): Int {
        return R.layout.activity_main_material3
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.toolbar)
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
            startActivity(Intent(this, ShowBackgroundTasksActivityMaterial3::class.java))
        }

        // 获取后台内存压缩任务列表
        findViewById<Button>(R.id.getAppCompactItemsBtn, moduleActive)?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowAppCompactListActivityMaterial3::class.java))
        }

        // 转去设置应用进程页面
        findViewById<Button>(
            R.id.gotoConfigureAppProcessActivityBtn, moduleActive
        )?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowAllInstalledAppsActivityMaterial3::class.java))
        }
    }
}