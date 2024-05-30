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

import android.content.Context
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.AppItem.AppConfiguredEnum
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy.SubProcessOomPolicyEnum
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
import com.venus.backgroundopt.utils.getView
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.handle.SubProcessOomConfigChangeMessageHandler
import com.venus.backgroundopt.utils.message.messageSender
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.preference.prefEdit
import com.venus.backgroundopt.utils.preference.prefPut
import com.venus.backgroundopt.utils.preference.putObject

/**
 * @author XingC
 * @date 2023/9/28
 */
object ConfigureAppProcessDialogBuilder {
    fun createDialog(
        context: Context,
        applyConfigNameTextView: TextView,
        processName: String,
        subProcessOomPolicy: SubProcessOomPolicy,
        subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>,
        appItem: AppItem
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setCancelable(true)
            .setView(context.getView(R.layout.dialog_sub_process_policy_choose).apply {
                val curPolicy = subProcessOomPolicy.policyEnum

                // 获取按钮
                val defaultBtnId = R.id.subProcessOomPolicyDialogDefaultBtn
                val mainProcessBtnId = R.id.subProcessOomPolicyDialogMainProcessBtn

                // 选中当前配置对应的按钮
                when (subProcessOomPolicy.policyEnum) {
                    SubProcessOomPolicyEnum.DEFAULT -> findViewById<RadioButton>(
                        defaultBtnId
                    ).isChecked = true

                    SubProcessOomPolicyEnum.MAIN_PROCESS -> findViewById<RadioButton>(
                        mainProcessBtnId
                    ).isChecked = true
                }

                // 设置点击事件
                findViewById<RadioGroup>(R.id.subProcessOomPolicyDialogRadioGroup).setOnCheckedChangeListener { _, checkedBtnResId ->
                    when (checkedBtnResId) {
                        defaultBtnId -> {
                            if (curPolicy != SubProcessOomPolicyEnum.DEFAULT) {
                                subProcessOomPolicy.policyEnum = SubProcessOomPolicyEnum.DEFAULT

                                subProcessOomPolicyMap.remove(processName)

                                // 删除原有配置
                                context.prefEdit(SUB_PROCESS_OOM_POLICY, commit = true) {
                                    if (CommonProperties.subProcessDefaultUpgradeSet.contains(
                                            processName
                                        )
                                    ) {
                                        // 白名单进程则覆盖设置
                                        putObject(processName, subProcessOomPolicy)
                                    } else {
                                        remove(processName)
                                    }
                                }
                            }
                        }

                        mainProcessBtnId -> {
                            if (curPolicy != SubProcessOomPolicyEnum.MAIN_PROCESS) {
                                subProcessOomPolicyMap[processName] = subProcessOomPolicy
                                context.prefPut(
                                    SUB_PROCESS_OOM_POLICY,
                                    commit = true,
                                    processName,
                                    subProcessOomPolicy.apply {
                                        policyEnum =
                                            SubProcessOomPolicyEnum.MAIN_PROCESS
                                    })
                            }
                        }
                    }

                    // 更新tag的显示
                    appItem.appConfiguredEnumSet.apply {
                        subProcessOomPolicyMap.values.find {
                            it.policyEnum != SubProcessOomPolicyEnum.DEFAULT
                        }?.let {
                            add(AppConfiguredEnum.SubProcessOomPolicy)
                        } ?: remove(AppConfiguredEnum.SubProcessOomPolicy)
                    }

                    // 修改外显的配置名称
                    applyConfigNameTextView.text = subProcessOomPolicy.policyEnum.configName

                    // 向模块主进程发送消息
                    messageSender.autoDetectUseThread {
                        sendMessage<SubProcessOomConfigChangeMessageHandler.SubProcessOomConfigChangeMessage>(
                            context,
                            MessageKeyConstants.subProcessOomConfigChange,
                            SubProcessOomConfigChangeMessageHandler.SubProcessOomConfigChangeMessage()
                                .apply {
                                    this.processName = processName
                                    this.subProcessOomPolicy = subProcessOomPolicy
                                }
                        )
                    }
                }
            })
            .create()
    }
}