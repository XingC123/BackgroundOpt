package com.venus.backgroundopt.ui.base

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.ui.widget.showProgressBarViewForAction
import com.venus.backgroundopt.utils.getIntentData
import com.venus.backgroundopt.utils.getTargetApps

/**
 * @author XingC
 * @date 2023/9/25
 */
abstract class ShowInfoFromAppItemActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction(this, "正在加载...") {
            init()
        }
    }

    abstract fun getShowInfoAdapter(appItems: List<AppItem>): ShowInfoFromAppItemAdapter
    abstract fun getRecyclerViewResId():Int

    open fun getToolBarTitle(): String {
        return ""
    }

    private fun init() {
        val stringExtra = getIntentData(intent)
        stringExtra ?: return

        val list = JSON.parseArray(stringExtra, BaseProcessInfoKt::class.java)
        list ?: return

        val appItems = getTargetApps(this, list)

        runOnUiThread {
            findViewById<RecyclerView>(getRecyclerViewResId()).apply {
                layoutManager = LinearLayoutManager(this@ShowInfoFromAppItemActivity).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                adapter = getShowInfoAdapter(appItems)
            }
        }
    }
}