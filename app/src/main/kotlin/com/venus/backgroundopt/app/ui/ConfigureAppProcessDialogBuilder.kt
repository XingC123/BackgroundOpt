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

package com.venus.backgroundopt.app.ui

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.ui.base.BaseActivity
import com.venus.backgroundopt.app.ui.base.ifVersionIsCompatible
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.common.entity.AppItem
import com.venus.backgroundopt.common.entity.AppItem.AppConfiguredEnum
import com.venus.backgroundopt.common.entity.message.SubProcessOomConfigChangeMessage
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy.SubProcessOomPolicyEnum
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.environment.constants.PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
import com.venus.backgroundopt.common.util.ifTrue
import com.venus.backgroundopt.common.util.message.MessageKeyConstants
import com.venus.backgroundopt.common.util.message.messageSender
import com.venus.backgroundopt.common.util.message.sendMessage
import com.venus.backgroundopt.common.util.preference.prefEdit
import com.venus.backgroundopt.common.util.preference.prefPut
import com.venus.backgroundopt.common.util.preference.putObject
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.entity.self.ProcessAdjConstants
import kotlin.reflect.KMutableProperty1

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
        appItem: AppItem,
    ): AlertDialog {
        // 获取按钮
        val defaultBtnId = R.id.subProcessOomPolicyDialogDefaultBtn
        val mainProcessBtnId = R.id.subProcessOomPolicyDialogMainProcessBtn
        val customAdjBtnId = R.id.subProcessOomPolicyDialogCustomBtn
        val customAdjComponentAndPropertyMap =
            HashMap<TextView, KMutableProperty1<SubProcessOomPolicy, Int>>()

        // 若用户点击单选按钮, 则更新此值
        var checkedRadioButtonId: Int = Int.MIN_VALUE

        return UiUtils.createDialog(
            context = context,
            cancelable = false,
            titleStr = "子进程配置策略",
            autoDismissAfterClickButton = false,
            viewResId = R.layout.dialog_sub_process_policy_choose,
            viewBlock = {
                val customFgAdjEditText = findViewById<TextView>(R.id.fg_adj_editText)
                val customBgAdjEditText = findViewById<TextView>(R.id.bg_adj_editText)
                val inputLayout = findViewById<LinearLayout>(R.id.adj_input_layout)

                // 初始化单选按钮的选择状态
                checkedRadioButtonId = when (subProcessOomPolicy.policyEnum) {
                    SubProcessOomPolicyEnum.DEFAULT -> {
                        findViewById<RadioButton>(
                            defaultBtnId
                        ).isChecked = true
                        defaultBtnId
                    }

                    SubProcessOomPolicyEnum.MAIN_PROCESS -> {
                        findViewById<RadioButton>(
                            mainProcessBtnId
                        ).isChecked = true
                        mainProcessBtnId
                    }

                    SubProcessOomPolicyEnum.CUSTOM_ADJ -> {
                        findViewById<RadioButton>(customAdjBtnId).isChecked = true
                        // 读取配置
                        ProcessAdjConstants.isValidAdj(subProcessOomPolicy.targetFgAdj).ifTrue {
                            customFgAdjEditText.text = subProcessOomPolicy.targetFgAdj.toString()
                        }
                        ProcessAdjConstants.isValidAdj(subProcessOomPolicy.targetBgAdj).ifTrue {
                            customBgAdjEditText.text = subProcessOomPolicy.targetBgAdj.toString()
                        }
                        // 显示adj输入域
                        inputLayout.visibility = View.VISIBLE

                        customAdjBtnId
                    }
                }

                // 设置点击事件
                findViewById<RadioGroup>(R.id.subProcessOomPolicyDialogRadioGroup).setOnCheckedChangeListener { _, checkedBtnResId ->
                    checkedRadioButtonId = checkedBtnResId

                    inputLayout.visibility = if (checkedBtnResId == customAdjBtnId) {
                        // ConfigureAppProcessAdapter2传入的context为BaseActivity
                        (context as? BaseActivity)?.ifVersionIsCompatible(
                            targetVersionCode = 207,
                            isNeedModuleRunning = true,
                            isForcible = false,
                        ) {}

                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }

                customAdjComponentAndPropertyMap.apply {
                    put(customFgAdjEditText, SubProcessOomPolicy::targetFgAdj)
                    put(customBgAdjEditText, SubProcessOomPolicy::targetBgAdj)
                }
            },
            enableNegativeBtn = true,
            enablePositiveBtn = true,
            positiveBlock = positiveBlock@{ dialogInterface: DialogInterface, _: Int ->
                if (checkedRadioButtonId == Int.MIN_VALUE) {
                    dialogInterface.dismiss()
                    return@positiveBlock
                }

                val curPolicy = subProcessOomPolicy.policyEnum
                when (checkedRadioButtonId) {
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

                    customAdjBtnId -> {
                        // 检验分数是否有效
                        var validAdjCount = 0
                        val arraySize = customAdjComponentAndPropertyMap.size
                        val componentArray = arrayOfNulls<TextView>(arraySize)
                        val adjArray = IntArray(arraySize)

                        customAdjComponentAndPropertyMap.keys.forEachIndexed { index, component ->
                            val customAdj = runCatchThrowable(defaultValue = Int.MIN_VALUE) {
                                component.text.toString().toInt()
                            }!!
                            if (ProcessAdjConstants.isValidAdj(customAdj)) {
                                ++validAdjCount
                            }
                            componentArray[index] = component
                            adjArray[index] = customAdj
                        }
                        // 全部无效
                        if (validAdjCount == 0) {
                            UiUtils.createDialog(
                                context = context,
                                textResId = R.string.invalid_custom_adj,
                                enablePositiveBtn = true,
                            ).show()
                            return@positiveBlock
                        }

                        // 分数没问题
                        subProcessOomPolicyMap[processName] = subProcessOomPolicy.apply {
                            policyEnum = SubProcessOomPolicyEnum.CUSTOM_ADJ

                            componentArray.forEachIndexed { index, component ->
                                customAdjComponentAndPropertyMap[component]!!.set(
                                    subProcessOomPolicy,
                                    adjArray[index]
                                )
                            }
                        }
                        context.prefPut(
                            SUB_PROCESS_OOM_POLICY,
                            commit = true,
                            processName,
                            subProcessOomPolicy
                        )
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
                    sendMessage<SubProcessOomConfigChangeMessage>(
                        context = context,
                        key = MessageKeyConstants.subProcessOomConfigChange,
                        value = SubProcessOomConfigChangeMessage().apply {
                            this.processName = processName
                            this.subProcessOomPolicy = subProcessOomPolicy
                        }
                    )
                }

                dialogInterface.dismiss()
            }
        )
    }
}