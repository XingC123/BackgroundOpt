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

package com.venus.backgroundopt.ui.base

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.ui.style.RecycleViewItemSpaceDecoration
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.getIntentData
import com.venus.backgroundopt.utils.showProgressBarViewForAction

/**
 * @author XingC
 * @date 2023/9/25
 */
abstract class ShowInfoFromAppItemActivityMaterial3 : BaseActivityMaterial3() {
    lateinit var appItems: MutableList<AppItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction("正在加载...") {
            init()
        }
    }

    abstract fun getShowInfoAdapter(
        appItems: MutableList<AppItem>,
        vararg others: Any?
    ): ShowInfoFromAppItemAdapter

    abstract fun getRecyclerViewResId(): Int

    open fun getToolBarTitle(): String {
        return ""
    }

    protected open fun init() {
        val stringExtra = getIntentData(intent)
        stringExtra ?: return

        val list = JSON.parseArray(stringExtra, BaseProcessInfoKt::class.java)
        list ?: return

        appItems = PackageUtils.getTargetApps(this, list)
        initRecyclerView(appItems = appItems)
    }

    protected open fun initRecyclerView(appItems: MutableList<AppItem>, vararg others: Any?) {
        runOnUiThread {
            findViewById<RecyclerView>(getRecyclerViewResId()).apply {
                layoutManager = LinearLayoutManager(this@ShowInfoFromAppItemActivityMaterial3).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                adapter = getShowInfoAdapter(appItems, *others)
                addItemDecoration(RecycleViewItemSpaceDecoration(context))
            }
        }
    }
}