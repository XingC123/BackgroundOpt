package com.venus.backgroundopt.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
        setContentView(R.layout.activity_show_all_installer_apps)

        showProgressBarViewForAction(this, "正在获取已安装应用...") {
            init()
        }
    }

    private fun init() {
        // app展示区
        val recyclerView = findViewById<RecyclerView>(R.id.showAllInstalledAppsRecycleView)
        // 初始化搜索栏
        val searchAppNameText = findViewById<EditText>(R.id.showAllInstalledAppsSearchText)
        findViewById<Button>(R.id.showAllInstalledAppsSearchBtn).setOnClickListener { _ ->
            searchAppNameText.text.toString().apply {
                (recyclerView.adapter as ShowAllInstalledAppsAdapter).filter.filter(this)
            }
        }

        val appItems = getInstalledPackages(this) { packageInfo ->
            !ActivityManagerService.isImportantSystemApp(packageInfo.applicationInfo)
        }

        runOnUiThread {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@ShowAllInstalledAppsActivity).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                adapter = ShowAllInstalledAppsAdapter(appItems)

                // 设置搜索栏隐藏/显示行为
//                setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//                    val firstCompletelyVisibleItemPosition =
//                        (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
//                    if (oldScrollY > 0) { // 上划
//                        if (firstCompletelyVisibleItemPosition > 1) { // 设置为不可见
//
//                        }
//                    } else if (oldScrollY < 0) { // 下滑
//                        if (firstCompletelyVisibleItemPosition >= 0) { // 设置为可见
//
//                        }
//                    }
//                }
            }
        }
    }
}