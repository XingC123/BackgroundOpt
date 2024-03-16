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

import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.utils.UiUtils

/**
 * @author XingC
 * @date 2023/11/3
 */
class FeatureActivity : BaseActivity() {
    override fun getContentView(): Int {
        return R.layout.activity_feature
    }

    override fun initToolBar(): Toolbar? {
        return UiUtils.getToolbar(
            this,
            R.id.toolbarLeftTitleToolbar,
            titleResId = R.string.menu_main_activity_toolbar_feature,
            enableCancelBtn = true
        )
    }
}