package com.venus.backgroundopt.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.ui.style.RecycleViewItemSpaceDecoration
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.showProgressBarViewForAction

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

        showProgressBarViewForAction("正在获取已安装应用...") {
            init()
        }
    }

    override fun getContentView(): Int {
        return R.layout.activity_show_all_installed_apps
    }

    override fun getToolBarMenusResId(): Int {
        return R.menu.menu_installed_app_toolbar
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.installerAppToolbarHelpMenuItem -> {
                UiUtils.createDialog(this, R.layout.content_installed_app_toolbar_help).show()
            }
        }
    }

    private fun init() {
        // app展示区
        val recyclerView = findViewById<RecyclerView>(R.id.showAllInstalledAppsRecycleView)
        // 初始化搜索栏
        val searchAppNameText = findViewById<EditText>(R.id.showAllInstalledAppsSearchText)
        findViewById<Button>(R.id.showAllInstalledAppsSearchBtn).setOnClickListener { _ ->
            searchAppNameText.text.toString().apply {
                (recyclerView.adapter as ShowAllInstalledAppsAdapter2).filter.filter(this)
            }
        }

        val appItems = PackageUtils.getInstalledPackages(this) { packageInfo ->
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
                adapter = ShowAllInstalledAppsAdapter2(appItems)
                addItemDecoration(RecycleViewItemSpaceDecoration(context))

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