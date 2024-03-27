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
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.utils.preference.prefBoolean

/**
 * 在使用时, 仅需选择性重写此类中的方法。父类中的方法不必再次重写, 除非它是安卓的方法
 *
 * @author XingC
 * @date 2023/10/1
 */
abstract class BaseActivityMaterial3 : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        super.onCreate(savedInstanceState)
    }

    private fun initTheme() {
        USE_DYNAMIC_THEME ?: run {
            // 使用动态取色
            val useDynamicTheme = prefBoolean(
                name = PreferenceNameConstants.MAIN_SETTINGS,
                key = PreferenceKeyConstants.DYNAMIC_THEME,
                defaultValue = false
            )
            USE_DYNAMIC_THEME = useDynamicTheme
        }

        if (USE_DYNAMIC_THEME == true) {
            theme.applyStyle(R.style.Theme_AppTheme_Dynamic, true)
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 工具栏                                                                   *
     *                                                                         *
     **************************************************************************/
    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.toolbar)
    }

    /**
     * 设置菜单项的点击监听
     *
     * @param menuItem 被点击的菜单项
     */
    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
    }

    /**
     * 设置ToolBar的返回按钮事件
     *
     * @param view
     */
    override fun setNavigationOnClickListener(view: View) {
        finish()
    }

    /* *************************************************************************
     *                                                                         *
     * Activity布局的ViewId                                                     *
     *                                                                         *
     **************************************************************************/
    abstract override fun getContentView(): Int

    companion object {
        // 动态取色
        @JvmField
        var USE_DYNAMIC_THEME: Boolean? = null
    }
}