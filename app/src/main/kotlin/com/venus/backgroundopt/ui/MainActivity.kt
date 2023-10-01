package com.venus.backgroundopt.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.venus.backgroundopt.R
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.ui.widget.QueryInfoDialog
import com.venus.backgroundopt.utils.findViewById
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.setIntentData


class MainActivity : AppCompatActivity(), ILogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        init()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.MainActivityAboutMenuItem -> startActivity(Intent(this, AboutAppActivity::class.java))
        }
        return true
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
            val listStr = sendMessage(
                this,
                MessageKeyConstants.getBackgroundTasks,
            )
            startActivity(Intent(this, ShowBackgroundTasksActivity::class.java).apply {
                setIntentData(this, listStr)
            })
        }

        // 获取后台内存压缩任务列表
        findViewById<Button>(R.id.getAppCompactItemsBtn, moduleActive)?.setOnClickListener { _ ->
            val listStr = sendMessage(
                this,
                MessageKeyConstants.getAppCompactList,
            )
            startActivity(Intent(this, ShowAppCompactListActivity::class.java).apply {
                setIntentData(this, listStr)
            })
        }

        // 转去设置应用进程页面
        findViewById<Button>(
            R.id.gotoConfigureAppProcessActivityBtn, moduleActive
        )?.setOnClickListener { _ ->
            startActivity(Intent(this, ShowAllInstalledAppsActivity::class.java))
        }
    }
}