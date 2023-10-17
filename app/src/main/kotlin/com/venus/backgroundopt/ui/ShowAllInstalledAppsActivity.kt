package com.venus.backgroundopt.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.ui.widget.showProgressBarViewForAction
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.getInstalledPackages

/**
 * @author XingC
 * @date 2023/9/27
 */
class ShowAllInstalledAppsActivity : BaseActivity() {
    override fun initToolBar(): Toolbar? {
        return UiUtils.getToolbar(this, R.id.toolbarLeftTitleToolbar, titleStr = "已安装应用")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction(this, "正在获取已安装应用...") {
            init()
        }
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_all_installed_apps
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
//        sendMessageAcceptList<AppItem>(
//            this,
//            MessageKeyConstants.getInstalledApps
//        )?.let { appItems ->
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
//            }
        }


    }
}