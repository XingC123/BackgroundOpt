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
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AllPreference
import com.venus.backgroundopt.ui.base.PreferenceActivityMaterial3

/**
 * @author XingC
 * @date 2023/10/16
 */
class SettingsActivityMaterial3 : PreferenceActivityMaterial3() {
    private lateinit var allPreference: AllPreference

    override fun preferenceFragmentContainerResId(): Int {
        return R.id.settingsActivityContainer
    }

    override fun getContentView(): Int {
        return R.layout.activity_settings_material3
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