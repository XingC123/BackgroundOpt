package com.venus.backgroundopt.ui.widget.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.getIntentData
import com.venus.backgroundopt.utils.getTargetApps

/**
 * @author XingC
 * @date 2023/9/25
 */
abstract class ShowInfoFromAppItemActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        init()
    }

    abstract fun getLayoutResId(): Int
    abstract fun getShowInfoAdapter(appItems: List<AppItem>): ShowInfoFromAppItemAdapter

    private fun init() {
        val stringExtra = getIntentData(intent)
        stringExtra ?: return

        val list = JSON.parseArray(stringExtra, ProcessRecord::class.java)
        list ?: return

        val appItems = getTargetApps(this, list)

        val layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            this.layoutManager = layoutManager
            adapter = getShowInfoAdapter(appItems)
        }
    }
}