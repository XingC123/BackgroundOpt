package com.venus.backgroundopt.ui.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.R
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.getTargetApps


/**
 * 用于展示后台任务列表的Activity
 *
 * @author XingC
 * @date 2023/9/23
 */
class ShowBackgroundTasksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_background_tasks_recycler_view)

        init()
    }


    private fun init() {
        val stringExtra = intent.getStringExtra("data")
        stringExtra ?: return

        val list = JSON.parseArray(stringExtra, ProcessRecord::class.java)
        list ?: return

        val appItems = getTargetApps(this, list)

        val layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            this.layoutManager = layoutManager
            adapter = GetBackgroundTasksAdapter(appItems)
        }
    }
}