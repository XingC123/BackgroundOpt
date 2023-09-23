package com.venus.backgroundopt.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.venus.backgroundopt.R
import com.venus.backgroundopt.ui.widget.QueryInfoDialog
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.message.MessageKeyConstants


class MainActivity : AppCompatActivity(), ILogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init() {
        // 查询运行中app的信息
        findViewById<Button>(R.id.getRunningAppInfoBtn)?.setOnClickListener { _ ->
            QueryInfoDialog.createQueryIntDataDialog(this, MessageKeyConstants.getRunningAppInfo)
                .show()
        }

        // 获取应用内存分组
        findViewById<Button>(R.id.getTargetAppGroupBtn)?.setOnClickListener { _ ->
            QueryInfoDialog.createQueryIntDataDialog(this, MessageKeyConstants.getTargetAppGroup)
                .show()
        }
    }
}