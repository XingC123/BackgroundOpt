package com.venus.backgroundopt.ui

import android.os.Bundle
import androidx.preference.ListPreference
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

        // 前台进程内存紧张
        val foregroundProcTrimMemPreference =
            findPreference<ListPreference>(PreferenceKeyConstants.FOREGROUND_PROC_TRIM_MEM_POLICY)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    preferenceChangeAction {
                        var key: String? = null
                        val index = findIndexOfValue(newValue as String)
                        if (index >= 0) {
                            key = entries[index].toString()
                        }
                        key?.let {
                            sendMessage(
                                requireActivity(),
                                MessageKeyConstants.foregroundProcTrimMemPolicy,
                                Pair(key, newValue)
                            )
                        }
                    }
                    true
                }
            }

        findPreference<SwitchPreference>(PreferenceKeyConstants.ENABLE_FOREGROUND_PROC_TRIM_MEM_POLICY)?.let { switchPreference ->
            val activity = requireActivity()
            foregroundProcTrimMemPreference?.let {
                activity.runOnUiThread {
                    it.isEnabled = switchPreference.isChecked
                }
            }

            switchPreference.setOnPreferenceChangeListener { _, newValue ->
                preferenceChangeAction {
                    foregroundProcTrimMemPreference?.let {
                        activity.runOnUiThread {
                            it.isEnabled = newValue as Boolean
                        }
                    }
                    sendMessage(
                        activity,
                        MessageKeyConstants.enableForegroundProcTrimMemPolicy,
                        newValue
                    )
                }
                true
            }
        }
    }
}