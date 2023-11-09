package com.venus.backgroundopt.entity

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.utils.JsonUtils
import com.venus.backgroundopt.utils.UiUtils
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
            runCatching {
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
                        UiUtils.createDialog(activity.baseContext, text = "备份失败, 文件流为空").show()
                    }
                }
            }.onFailure {
                UiUtils.createDialog(
                    activity.baseContext,
                    text = "备份失败, 错误信息: ${it.message}"
                ).show()
            }.onSuccess {
                UiUtils.createDialog(activity.baseContext, text = "备份成功").show()
            }
        }

    private val restoreLauncher =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult

            runCatching {
                activity.contentResolver.openInputStream(uri)
                    ?.reader(StandardCharsets.UTF_8).use { inputStream ->
                        inputStream?.readText()
                    }?.let { json ->
                        val allPreference = JsonUtils.parseObject(json, JsonPreference::class.java)
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
                UiUtils.createDialog(
                    activity.baseContext,
                    text = "恢复备份失败, 错误信息: ${it.message}"
                ).show()
            }.onSuccess {
                UiUtils.createDialog(activity.baseContext, text = "恢复备份成功").show()
            }
        }


    fun backup() {
        backupLauncher.launch("backgroundOpt.json")
    }

    fun restore() {
        restoreLauncher.launch("application/json")
    }

    class JsonPreference {
        lateinit var appOptimizePolicyMap: MutableMap<String, AppOptimizePolicy>
        lateinit var subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>
        lateinit var mainSettingsMap: MutableMap<String, *>
    }
}