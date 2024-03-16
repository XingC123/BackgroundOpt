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
                    
 package com.venus.backgroundopt.entity

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.utils.JsonUtils
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.concurrent.newThreadTaskResult
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.preference.prefAll
import com.venus.backgroundopt.utils.preference.prefEdit
import com.venus.backgroundopt.utils.preference.putObject
import java.nio.charset.StandardCharsets


/**
 * @author XingC
 * @date 2023/11/8
 */
class AllPreference(private val activity: ComponentActivity) {
    private val backupLauncher =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri ?: return@registerForActivityResult

            newThreadTaskResult {
                activity.contentResolver.openOutputStream(uri).use { outputStream ->
                    val context = activity.baseContext
                    val jsonPreference = JsonPreference()
                    jsonPreference.mainSettingsMap =
                        context.prefAll(PreferenceNameConstants.MAIN_SETTINGS)
                    jsonPreference.appOptimizePolicyMap =
                        context.prefAll<AppOptimizePolicy>(PreferenceNameConstants.APP_OPTIMIZE_POLICY)
                    jsonPreference.subProcessOomPolicyMap =
                        context.prefAll<SubProcessOomPolicy>(PreferenceNameConstants.SUB_PROCESS_OOM_POLICY)
                    val jsonString = JsonUtils.toJsonString(jsonPreference)
                    outputStream?.write(jsonString.toByteArray(StandardCharsets.UTF_8)) ?: run {
                        throw NullPointerException("文件流为空")
                    }
                }
            }.onFailure {
                showDialog("备份失败, 错误信息: ${it.message}")
            }.onSuccess {
                showDialog("备份成功")
            }
        }

    private val restoreLauncher =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult

            newThreadTaskResult {
                activity.contentResolver.openInputStream(uri)
                    ?.reader(StandardCharsets.UTF_8).use { inputStream ->
                        inputStream?.readText()
                    }?.let { json ->
                        val allPreference =
                            JsonUtils.parseObject(json, JsonPreference::class.java)
                        // 写入配置
                        val context = activity.baseContext

                        context.prefEdit(
                            PreferenceNameConstants.APP_OPTIMIZE_POLICY,
                            commit = true
                        ) {
                            allPreference.appOptimizePolicyMap.forEach(this::putObject)
                        }

                        context.prefEdit(
                            PreferenceNameConstants.SUB_PROCESS_OOM_POLICY,
                            commit = true
                        ) {
                            allPreference.subProcessOomPolicyMap.forEach(this::putObject)
                        }

                        context.prefEdit(PreferenceNameConstants.MAIN_SETTINGS, commit = true) {
                            allPreference.mainSettingsMap.forEach { (prefKey, value) ->
                                when (value) {
                                    is Boolean -> putBoolean(prefKey, value)
                                    is String -> putString(prefKey, value)
                                }
                            }
                        }
                    }
            }.onFailure {
                showDialog("恢复备份失败, 错误信息: ${it.message}")
            }.onSuccess {
                showDialog(text = "恢复备份成功, 重启生效")
            }
        }


    fun backup() {
        backupLauncher.launch("backgroundOpt.json")
    }

    fun restore() {
        restoreLauncher.launch("application/json")
    }

    private fun showDialog(text: String) {
        activity.runOnUiThread {
            UiUtils.createDialog(
                activity,
                text = text,
                enablePositiveBtn = true,
                positiveBlock = { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
            ).show()
        }
    }

    class JsonPreference : MessageFlag {
        lateinit var appOptimizePolicyMap: MutableMap<String, AppOptimizePolicy>
        lateinit var subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>
        lateinit var mainSettingsMap: MutableMap<String, *>
    }
}