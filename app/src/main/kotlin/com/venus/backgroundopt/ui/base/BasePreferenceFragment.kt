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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.venus.backgroundopt.R
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.utils.ifTrue
import com.venus.backgroundopt.utils.preference.pref
import com.venus.backgroundopt.utils.runCatchThrowable
import com.venus.backgroundopt.utils.showProgressBarViewForAction

/**
 * PreferenceFragment的拓展类
 *
 * @author XingC
 * @date 2023/10/16
 */
open class BasePreferenceFragment<T : Activity> : PreferenceFragmentCompat() {
    private var preference: SharedPreferences? = null

    @SuppressLint("WorldReadableFiles")
    @Suppress("UNCHECKED_CAST")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (CommonProperties.isModuleActive()) {
            preferenceManager.sharedPreferencesName = PreferenceNameConstants.MAIN_SETTINGS
            preferenceManager.sharedPreferencesMode = Context.MODE_WORLD_READABLE
            addPreferencesFromResource(R.xml.preference_settings)
        } else {
            addPreferencesFromResource(R.xml.preference_settings)
            preferenceManager.preferenceScreen.isEnabled = false
        }

        preference =
            (requireActivity() as? T)?.pref(PreferenceNameConstants.MAIN_SETTINGS)
                ?: run {
                    preferenceManager.preferenceScreen.isEnabled = false
                    null
                }
    }

    fun preferenceChangeAction(
        text: String = "正在设置...",
        throwException: Boolean = false,
        action: () -> Unit
    ) {
        requireContext().showProgressBarViewForAction(
            text,
            enableNegativeBtn = false
        ) {
            runCatchThrowable(catchBlock = { throwable ->
                throwException.ifTrue {
                    throw throwable
                }
            }) {
                action()
            }
        }
    }
}