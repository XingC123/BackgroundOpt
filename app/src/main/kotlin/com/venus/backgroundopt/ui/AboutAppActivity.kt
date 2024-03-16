/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
                    
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
                resources.getStringArray(R.array.about_app_thanks_desc).toList(),
                resources.getStringArray(R.array.about_app_thanks_url).toList()
            )
        }
    }
}