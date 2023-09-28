package com.venus.backgroundopt.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.ui.widget.showProgressBarViewForAction
import com.venus.backgroundopt.utils.getInstalledPackages

/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_app_view)

        showProgressBarViewForAction(this, "正在获取已安装应用...") {
            init()
        }
    }

    private fun init() {
        val appItems = getInstalledPackages(this) { packageInfo ->
            !ActivityManagerService.isImportantSystemApp(packageInfo.applicationInfo)
        }

        runOnUiThread {
            findViewById<RecyclerView>(R.id.recyclerView).apply {
                layoutManager = LinearLayoutManager(this@ShowAllInstalledAppsActivity).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                adapter = ShowAllInstalledAppsAdapter(appItems)
            }
        }
    }
}