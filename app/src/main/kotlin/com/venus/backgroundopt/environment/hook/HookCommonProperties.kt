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

package com.venus.backgroundopt.environment.hook

import com.venus.backgroundopt.entity.preference.OomWorkModePref
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.CommonProperties.subProcessDefaultUpgradeSet
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.reference.PropertyValueWrapper
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.log.logInfo
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.message.handle.ForegroundProcTrimMemLevelEnum
import com.venus.backgroundopt.utils.message.handle.ForegroundProcTrimMemPolicy
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.utils.message.handle.GlobalOomScorePolicy
import com.venus.backgroundopt.utils.preference.PreferencesUtil
import com.venus.backgroundopt.utils.preference.prefAll
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2024/4/17
 */
object HookCommonProperties : ILogger {
    private fun printPreferenceActiveState(isEnabled: Boolean, description: String) {
        logger.info("[${if (isEnabled) "启用" else "禁用"}] ${description}")
    }

    // 子进程oom策略映射表
    val subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy> by lazy {
        (prefAll(PreferenceNameConstants.SUB_PROCESS_OOM_POLICY)
            ?: ConcurrentHashMap<String, SubProcessOomPolicy>()).apply {
            subProcessDefaultUpgradeSet.forEach { processName ->
                if (!this.containsKey(processName)) {
                    this[processName] = SubProcessOomPolicy().apply {
                        policyEnum = SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
                    }
                }
            }
        }
    }

    fun getUpgradeSubProcessNames(): Set<String> {
        return HashSet<String>().apply {
            subProcessOomPolicyMap.forEach { (processName, policy) ->
                if (policy.policyEnum == SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS) {
                    add(processName)
                }
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * OOM                                                                     *
     *                                                                         *
     **************************************************************************/
    val oomWorkModePref by lazy {
        val oomWorkMode = PreferencesUtil.getString(
            PreferenceNameConstants.MAIN_SETTINGS,
            PreferenceKeyConstants.OOM_WORK_MODE,
            PreferenceDefaultValue.oomWorkMode.toString()
        )!!
        logInfo(logStr = "Oom工作模式: $oomWorkMode")
        OomWorkModePref(oomWorkMode.toInt())
    }

    /* *************************************************************************
     *                                                                         *
     * 进程压缩相关配置                                                           *
     *                                                                         *
     **************************************************************************/
    fun getAutoStopCompactTaskPreferenceValue(): Boolean {
        return PreferencesUtil.getBoolean(
            PreferenceNameConstants.MAIN_SETTINGS,
            PreferenceKeyConstants.AUTO_STOP_COMPACT_TASK
        )
    }

    /* *************************************************************************
     *                                                                         *
     * 进程内存紧张                                                              *
     *                                                                         *
     **************************************************************************/
    val foregroundProcTrimMemPolicy by lazy {
        val isEnabled = PreferencesUtil.getBoolean(
            PreferenceNameConstants.MAIN_SETTINGS,
            PreferenceKeyConstants.ENABLE_FOREGROUND_PROC_TRIM_MEM_POLICY,
            PreferenceDefaultValue.enableForegroundTrimMem
        )
        val enumName = PreferencesUtil.getString(
            path = PreferenceNameConstants.MAIN_SETTINGS,
            key = PreferenceKeyConstants.FOREGROUND_PROC_TRIM_MEM_POLICY,
            defaultValue = PreferenceDefaultValue.foregroundProcTrimMemLevelEnumName
        )!!
        val levelEnum = ForegroundProcTrimMemLevelEnum.valueOf(enumName)

        val policy = ForegroundProcTrimMemPolicy().apply {
            this.isEnabled = isEnabled
            this.foregroundProcTrimMemLevelEnum = levelEnum
        }

        PropertyValueWrapper(policy)
    }

    fun isEnableForegroundProcTrimMem() = foregroundProcTrimMemPolicy.value.isEnabled

    fun getForegroundProcTrimMemLevel() =
        foregroundProcTrimMemPolicy.value.foregroundProcTrimMemLevelEnum.level

    fun getForegroundProcTrimMemLevelUiName(): String =
        foregroundProcTrimMemPolicy.value.foregroundProcTrimMemLevelEnum.uiName

    val backgroundProcTrimMemPolicy by lazy {
        val isEnabled = PreferencesUtil.getBoolean(
            PreferenceNameConstants.MAIN_SETTINGS,
            PreferenceKeyConstants.ENABLE_BACKGROUND_PROC_TRIM_MEM_POLICY,
            PreferenceDefaultValue.enableBackgroundTrimMem
        )
        PropertyValueWrapper(isEnabled)
    }

    fun isEnableBackgroundProcTrimMem(): Boolean = backgroundProcTrimMemPolicy.value

    /* *************************************************************************
     *                                                                         *
     * 应用后台优化相关                                                           *
     *                                                                         *
     **************************************************************************/
    /**
     * app优化策略<packageName, [AppOptimizePolicy]>
     */
    val appOptimizePolicyMap: MutableMap<String, AppOptimizePolicy> by lazy {
        (prefAll(PreferenceNameConstants.APP_OPTIMIZE_POLICY)
            ?: ConcurrentHashMap<String, AppOptimizePolicy>())
    }

    @JvmStatic
    fun computeAppOptimizePolicy(userId: Int, packageName: String): AppOptimizePolicy {
        return AppOptimizePolicy().apply {
            this.packageName = packageName
        }
    }

    @JvmStatic
    fun computeAppOptimizePolicyInMap(userId: Int, packageName: String): AppOptimizePolicy {
        return appOptimizePolicyMap.computeIfAbsent(packageName) {
            computeAppOptimizePolicy(userId = userId, packageName = packageName)
        }
    }

    @JvmStatic
    @Deprecated(
        message = "AppOptimizePolicy.shouldHandleAdjUiState已被取代",
        replaceWith = ReplaceWith("mainProcessAdjManagePolicy")
    )
    fun setShouldHandleAdjUiState(userId: Int, packageName: String, shouldHandleAdjUiState: Boolean) {
        computeAppOptimizePolicyInMap(
            userId = userId,
            packageName = packageName
        ).shouldHandleAdjUiState = shouldHandleAdjUiState
    }

    /* *************************************************************************
     *                                                                         *
     * 拥有界面时临时保活主进程                                                     *
     *                                                                         *
     **************************************************************************/
    val keepMainProcessAliveHasActivityPolicy by lazy {
        val isEnabled = PreferencesUtil.getBoolean(
            path = PreferenceNameConstants.MAIN_SETTINGS,
            key = PreferenceKeyConstants.KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY,
            defaultValue = PreferenceDefaultValue.keepMainProcessAliveHasActivity
        )
        printKeepMainProcessAliveHasActivityInfo(isEnabled)
        PropertyValueWrapper(isEnabled)
    }

    @JvmStatic
    fun printKeepMainProcessAliveHasActivityInfo(isEnabled:Boolean) {
        printPreferenceActiveState(
            isEnabled = isEnabled,
            description = "拥有界面的app的主进程临时保活"
        )
    }

    @JvmStatic
    fun isEnableKeepMainProcessAliveHasActivity(): Boolean = keepMainProcessAliveHasActivityPolicy.value

    /* *************************************************************************
     *                                                                         *
     * webview进程的处理                                                         *
     *                                                                         *
     **************************************************************************/
    val enableWebviewProcessProtect by lazy {
        val propertyValueWrapper =
            PropertyValueWrapper(
                PreferencesUtil.getBoolean(
                    path = PreferenceNameConstants.MAIN_SETTINGS,
                    key = PreferenceKeyConstants.APP_WEBVIEW_PROCESS_PROTECT,
                    defaultValue = PreferenceDefaultValue.enableWebviewProcessProtect
                )
            )
        logger.info("[${if (propertyValueWrapper.value) "启用" else "禁用"}]Webview进程保护")
        propertyValueWrapper
    }

    /* *************************************************************************
     *                                                                         *
     * Simple Lmk                                                              *
     *                                                                         *
     **************************************************************************/
    val enableSimpleLmk by lazy {
        val propertyValueWrapper =
            PropertyValueWrapper(
                PreferencesUtil.getBoolean(
                    path = PreferenceNameConstants.MAIN_SETTINGS,
                    key = PreferenceKeyConstants.SIMPLE_LMK,
                    defaultValue = PreferenceDefaultValue.enableSimpleLmk
                )
            ).apply {
                addListener(PreferenceKeyConstants.SIMPLE_LMK) { _, isEnabled ->
                    useSimpleLmk = useSimpleLmk(isEnabled = isEnabled)
                }
            }
        propertyValueWrapper
    }

    @JvmStatic
    @Volatile
    var useSimpleLmk: Boolean = useSimpleLmk(isEnabled = enableSimpleLmk.value).also {
        printPreferenceActiveState(isEnabled = it, description = "Simple Lmk")
    }
        set(value) {
            field = value
            printPreferenceActiveState(isEnabled = value, description = "Simple Lmk")
        }

    /**
     * simple lmk 只在平衡模式生效
     * @param isEnabled Boolean 配置文件中的启用/禁用状态
     * @return Boolean 启用 -> true
     */
    @JvmStatic
    private fun useSimpleLmk(isEnabled: Boolean): Boolean {
        return isEnabled && (
                oomWorkModePref.oomMode == OomWorkModePref.MODE_BALANCE
                        || oomWorkModePref.oomMode == OomWorkModePref.MODE_BALANCE_PLUS
                )
    }

    /* *************************************************************************
     *                                                                         *
     * 全局OOM                                                                  *
     *                                                                         *
     **************************************************************************/
    val globalOomScorePolicy by lazy {
        val isEnabled = PreferencesUtil.getBoolean(
            path = PreferenceNameConstants.MAIN_SETTINGS,
            key = PreferenceKeyConstants.GLOBAL_OOM_SCORE,
            defaultValue = PreferenceDefaultValue.enableGlobalOomScore
        )
        val policy = GlobalOomScorePolicy().apply {
            if (!isEnabled) {
                return@apply
            }

            enabled = true
            globalOomScoreEffectiveScope = try {
                GlobalOomScoreEffectiveScopeEnum.valueOf(
                    PreferencesUtil.getString(
                        path = PreferenceNameConstants.MAIN_SETTINGS,
                        key = PreferenceKeyConstants.GLOBAL_OOM_SCORE_EFFECTIVE_SCOPE,
                        defaultValue = PreferenceDefaultValue.globalOomScoreEffectiveScopeName
                    )!!
                )
            } catch (t: Throwable) {
                enabled = false
                GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS
            }
            val scoreValue = try {
                PreferencesUtil.getString(
                    path = PreferenceNameConstants.MAIN_SETTINGS,
                    key = PreferenceKeyConstants.GLOBAL_OOM_SCORE_VALUE,
                    defaultValue = PreferenceDefaultValue.customGlobalOomScoreValue.toString()
                )!!.toInt()
            } catch (t: Throwable) {
                enabled = false
                PreferenceDefaultValue.customGlobalOomScoreValue
            }
            customGlobalOomScore = GlobalOomScorePolicy.getCustomGlobalOomScoreIfIllegal(
                score = scoreValue,
                defaultValue = PreferenceDefaultValue.customGlobalOomScoreValue
            )
        }
        logger.info(policy.toString())
        PropertyValueWrapper(policy)
    }

    /* *************************************************************************
     *                                                                         *
     * 划卡杀后台                                                                *
     *                                                                         *
     **************************************************************************/
    val enableKillAfterRemoveTask by lazy {
        val isEnabledValueWrapper = PropertyValueWrapper(
            PreferencesUtil.getBoolean(
                path = PreferenceNameConstants.MAIN_SETTINGS,
                key = PreferenceKeyConstants.KILL_AFTER_REMOVE_TASK,
                defaultValue = PreferenceDefaultValue.killAfterRemoveTask
            )
        ).apply {
            addListener(PreferenceKeyConstants.KILL_AFTER_REMOVE_TASK) { _, newValue ->
                printPreferenceActiveState(
                    isEnabled = newValue,
                    description = "划卡杀后台"
                )
            }
        }
        printPreferenceActiveState(
            isEnabled = isEnabledValueWrapper.value,
            description = "划卡杀后台"
        )
        isEnabledValueWrapper
    }
}