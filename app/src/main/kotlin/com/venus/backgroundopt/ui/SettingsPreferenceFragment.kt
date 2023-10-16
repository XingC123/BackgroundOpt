package com.venus.backgroundopt.ui

import android.os.Bundle
import androidx.preference.SwitchPreference
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.ui.base.BasePreferenceFragment
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.sendMessage

/**
 * @author XingC
 * @date 2023/10/16
 */
class SettingsPreferenceFragment : BasePreferenceFragment<SettingsActivity>() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        init()
    }

    private fun init() {
        findPreference<SwitchPreference>(PreferenceKeyConstants.AUTO_STOP_COMPACT_TASK)?.let { switchPreference ->
            switchPreference.setOnPreferenceChangeListener { _, newValue ->
                preferenceChangeAction {
                    sendMessage(
                        requireActivity(),
                        MessageKeyConstants.autoStopCompactTask,
                        newValue
                    )
                }
                true
            }
        }
    }
}