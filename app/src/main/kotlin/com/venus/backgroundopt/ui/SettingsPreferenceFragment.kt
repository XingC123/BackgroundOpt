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
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.ui.base.BasePreferenceFragment
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.utils.message.handle.GlobalOomScorePolicy
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
        initSwitchPreferenceChangeListener(
            switchPreference = findPreference(PreferenceKeyConstants.AUTO_STOP_COMPACT_TASK),
            messageKey = MessageKeyConstants.autoStopCompactTask,
        )

        // 后台进程内存紧张
        initSwitchPreferenceActiveAfterRestart(
            preferenceKey = PreferenceKeyConstants.ENABLE_BACKGROUND_PROC_TRIM_MEM_POLICY
        )

        // 前台进程内存紧张
        initListPreferenceChangeListener(
            preferenceKey = PreferenceKeyConstants.FOREGROUND_PROC_TRIM_MEM_POLICY,
            messageKey = MessageKeyConstants.foregroundProcTrimMemPolicy
        )
        initSwitchPreferenceChangeListener(
            preferenceKey = PreferenceKeyConstants.ENABLE_FOREGROUND_PROC_TRIM_MEM_POLICY,
            messageKey = MessageKeyConstants.enableForegroundProcTrimMemPolicy
        )

        // oom工作模式
        findPreference<ListPreference>(PreferenceKeyConstants.OOM_WORK_MODE)?.apply {
            setOnPreferenceChangeListener { _, _ ->
                UiUtils.createDialog(
                    requireActivity(),
                    text = "更改此配置需要重启生效",
                    enablePositiveBtn = true,
                    positiveBlock = { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                ).show()
                true
            }
        }

        // app的webview进程保护
        initSwitchPreferenceChangeListener(
            switchPreference = findPreference(PreferenceKeyConstants.APP_WEBVIEW_PROCESS_PROTECT),
            messageKey = MessageKeyConstants.appWebviewProcessProtect,
        )

        // simple lmk
        initSwitchPreferenceChangeListener(
            switchPreference = findPreference(PreferenceKeyConstants.SIMPLE_LMK),
            messageKey = MessageKeyConstants.enableSimpleLmk
        )

        /*
            全局OOM
         */
        val globalOomScoreEffectiveScopePreference = initListPreferenceChangeListener(
            preferenceKey = PreferenceKeyConstants.GLOBAL_OOM_SCORE_EFFECTIVE_SCOPE,
            messageKey = MessageKeyConstants.globalOomScoreEffectiveScope
        )

        val globalOomScoreValuePreference =
            findPreference<EditTextPreference>(PreferenceKeyConstants.GLOBAL_OOM_SCORE_VALUE)?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    var customOomScore: Int = Int.MIN_VALUE
                    val isValid = try {
                        (newValue as? String)?.toInt()?.let { score ->
                            customOomScore = score
                            GlobalOomScorePolicy.isCustomGlobalOomScoreIllegal(score)
                        } ?: false
                    } catch (t: Throwable) {
                        false
                    }
                    if (isValid) {
                        preferenceChangeAction {
                            sendMessage(
                                context = requireActivity(),
                                key = MessageKeyConstants.globalOomScoreValue,
                                value = customOomScore
                            )
                        }
                    } else {
                        UiUtils.createDialog(
                            context = requireActivity(),
                            text = "参数值不合法",
                            cancelable = false,
                            enablePositiveBtn = true,
                            positiveBlock = { dialogInterface, i ->
                                dialogInterface.dismiss()
                            }
                        ).show()
                    }

                    isValid
                }
            }

        initSwitchPreferenceChangeListener(
            switchPreference = findPreference(PreferenceKeyConstants.GLOBAL_OOM_SCORE),
            messageKey = MessageKeyConstants.enableGlobalOomScore,
        ) { _, value ->
            val isEnabled = (value as? Boolean) ?: return@initSwitchPreferenceChangeListener null
            val globalOomScorePolicy = GlobalOomScorePolicy()
            if (isEnabled) {
                globalOomScoreEffectiveScopePreference?.let { scopePref ->
                    globalOomScoreValuePreference?.let { valuePref ->
                        val scopeValue: Int
                        val scopeEnum: GlobalOomScoreEffectiveScopeEnum
                        try {
                            scopeValue = valuePref.text!!.toInt()
                            scopeEnum = GlobalOomScoreEffectiveScopeEnum.valueOf(scopePref.value)
                        } catch (t: Throwable) {
                            return@initSwitchPreferenceChangeListener null
                        }
                        return@initSwitchPreferenceChangeListener globalOomScorePolicy.apply {
                            enabled = true
                            globalOomScoreEffectiveScope = scopeEnum
                            customGlobalOomScore = scopeValue
                        }
                    }
                }
                null
            } else {
                globalOomScorePolicy
            }
        }

        // 划卡杀后台
        initSwitchPreferenceChangeListener(
            preferenceKey = PreferenceKeyConstants.KILL_AFTER_REMOVE_TASK,
            messageKey = MessageKeyConstants.killAfterRemoveTask
        )

        // 拥有界面时临时保活主进程
        initSwitchPreferenceChangeListener(
            preferenceKey = PreferenceKeyConstants.KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY,
            messageKey = MessageKeyConstants.KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY
        )
    }

    /**
     * 初始化功能的切换需要重启才能生效的[SwitchPreference]
     * @param preferenceKey String 配置项对应的key
     */
    private fun initSwitchPreferenceActiveAfterRestart(
        preferenceKey: String,
    ) {
        findPreference<SwitchPreference>(preferenceKey)?.apply {
            setOnPreferenceChangeListener { _, _ ->
                UiUtils.createDialog(
                    requireActivity(),
                    text = "更改此配置需要重启生效",
                    enablePositiveBtn = true,
                    positiveBlock = { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                ).show()
                true
            }
        }
    }

    private fun initSwitchPreferenceChangeListener(
        switchPreference: SwitchPreference?,
        messageKey: String,
    ) {
        switchPreference?.setOnPreferenceChangeListener { _, newValue ->
            preferenceChangeAction {
                sendMessage(
                    requireActivity(),
                    messageKey,
                    newValue
                )
            }
            true
        }
    }

    private fun initSwitchPreferenceChangeListener(
        preferenceKey: String,
        messageKey: String
    ): SwitchPreference? {
        return findPreference<SwitchPreference?>(preferenceKey)?.also { switchPreference ->
            initSwitchPreferenceChangeListener(switchPreference, messageKey)
        }
    }

    private fun initSwitchPreferenceChangeListener(
        switchPreference: SwitchPreference?,
        messageKey: String,
        messageValueBlock: (Preference, Any?) -> Any?
    ) {
        switchPreference?.setOnPreferenceChangeListener { preference, newValue ->
            messageValueBlock(preference, newValue)?.let { value ->
                preferenceChangeAction {
                    sendMessage(
                        requireActivity(),
                        messageKey,
                        value
                    )
                }
            }
            true
        }
    }

    private fun initSwitchPreferenceChangeListener(
        preferenceKey: String,
        messageKey: String,
        messageValue: (Preference, Any?) -> Any?
    ): SwitchPreference? {
        return findPreference<SwitchPreference>(preferenceKey)?.also {
            initSwitchPreferenceChangeListener(
                switchPreference = it,
                messageKey = messageKey,
                messageValueBlock = messageValue
            )
        }
    }

    private fun initListPreferenceChangeListener(
        preferenceKey: String,
        messageKey: String
    ): ListPreference? {
        return findPreference<ListPreference?>(preferenceKey)?.apply {
            initListPreferenceChangeListener(
                listPreference = this,
                messageKey = messageKey
            ) { _, value ->
                value
            }
        }
    }

    private fun initListPreferenceChangeListener(
        preferenceKey: String,
        messageKey: String,
        messageValueBlock: (Preference, Any?) -> Any?
    ): ListPreference? {
        return findPreference<ListPreference?>(preferenceKey)?.apply {
            initListPreferenceChangeListener(
                listPreference = this,
                messageKey = messageKey,
                messageValueBlock = messageValueBlock
            )
        }
    }

    private fun initListPreferenceChangeListener(
        listPreference: ListPreference?,
        messageKey: String,
        messageValueBlock: (Preference, Any?) -> Any?
    ) {
        listPreference?.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                preferenceChangeAction {
                    var key: String? = null
                    val index = findIndexOfValue(newValue as String)
                    if (index >= 0) {
                        key = entries[index].toString()
                    }
                    key?.let {
                        sendMessage(
                            requireActivity(),
                            messageKey,
                            Pair(key, messageValueBlock(preference, newValue))
                        )
                    }
                }
                true
            }
        }
    }
}