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
import com.venus.backgroundopt.ui.SettingsPreferenceFragment

/**
 * 需要实现设置页面的Activity可继承自本类
 *
 * @author XingC
 * @date 2023/10/16
 */
abstract class PreferenceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.beginTransaction()
            .add(preferenceFragmentContainerResId(), SettingsPreferenceFragment()).commit()
    }

    abstract fun preferenceFragmentContainerResId(): Int
}