package com.venus.backgroundopt.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.R
import com.venus.backgroundopt.ui.base.BaseActivity

/**
 * @author XingC
 * @date 2023/10/1
 */
class AboutAppActivity : BaseActivity() {
    override fun getContentView(): Int {
        return R.layout.activity_about_app
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.aboutAppToolBar)
    }

    fun init() {
        // 版本信息
        findViewById<TextView>(R.id.aboutAppVersionNameText)?.text = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.aboutAppVersionCodeText)?.text = BuildConfig.VERSION_CODE.toString()

        // 加载鸣谢名单
        findViewById<RecyclerView>(R.id.aboutAppThanksRecyclerView)?.apply {
            layoutManager = LinearLayoutManager(this@AboutAppActivity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            adapter = AboutAppThanksAdapter(
                resources.getStringArray(R.array.about_app_thanks_url).toList()
            )
        }
    }
}