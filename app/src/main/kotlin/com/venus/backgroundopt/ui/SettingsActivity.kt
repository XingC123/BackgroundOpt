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
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AllPreference
import com.venus.backgroundopt.ui.base.PreferenceActivity
import com.venus.backgroundopt.utils.UiUtils

/**
 * @author XingC
 * @date 2023/10/16
 */
@Deprecated(
    message = "默认使用Material3设计风格",
    replaceWith = ReplaceWith("SettingsActivityMaterial3")
)
class SettingsActivity : PreferenceActivity() {
    private lateinit var allPreference: AllPreference

    override fun preferenceFragmentContainerResId(): Int {
        return R.id.settingsActivityContainer
    }

    override fun getContentView(): Int {
        return R.layout.activity_settings
    }

    override fun initToolBar(): Toolbar? {
        return UiUtils.getToolbar(
            this,
            R.id.toolbarLeftTitleToolbar,
            titleResId = R.string.menu_main_activity_toolbar_settings,
            enableCancelBtn = true
        )
    }

    override fun getToolBarMenusResId(): Int {
        return R.menu.menu_settings_activity_toolbar
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.settingsToolbarBackupMenuItem -> {
                allPreference.backup()
            }

            R.id.settingsToolbarRestoreMenuItem -> {
                allPreference.restore()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        allPreference = AllPreference(this)
    }
}