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
import com.venus.backgroundopt.utils.preference.pref
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

    fun preferenceChangeAction(text: String = "正在设置...", action: () -> Unit) {
        requireContext().showProgressBarViewForAction(
            text,
            enableNegativeBtn = false,
            action = action
        )
    }
}