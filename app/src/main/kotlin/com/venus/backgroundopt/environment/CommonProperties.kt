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

package com.venus.backgroundopt.environment

import com.venus.backgroundopt.entity.preference.OomWorkModePref
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
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
 * @date 2023/9/26
 */
object CommonProperties : ILogger {
    // 模块是否激活
    fun isModuleActive(): Boolean {
        return false
    }

    private fun printPreferenceActiveState(isEnabled: Boolean, description: String) {
        logger.info("[${if (isEnabled) "启用" else "禁用"}] ${description}")
    }

    // 默认白名单
    val subProcessDefaultUpgradeSet: Set<String> by lazy {
        setOf(
            "com.tencent.mobileqq:MSF", /* qq */
            "com.tencent.mm:push", /* 微信 */
        )
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
            )
        logger.info("[${if (useSimpleLmk(isEnabled = propertyValueWrapper.value)) "启用" else "禁用"}]Simple Lmk")
        propertyValueWrapper
    }

    /**
     * simple lmk 只在平衡模式生效
     * @return Boolean 启用 -> true
     */
    @JvmStatic
    fun useSimpleLmk(): Boolean = useSimpleLmk(isEnabled = enableSimpleLmk.value)

    @JvmStatic
    private fun useSimpleLmk(isEnabled: Boolean): Boolean =
        isEnabled && oomWorkModePref.oomMode == OomWorkModePref.MODE_BALANCE

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