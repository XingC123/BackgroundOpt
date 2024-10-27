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

package com.venus.backgroundopt.xposed.environment

import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy
import com.venus.backgroundopt.common.entity.message.ForegroundProcTrimMemLevelEnum
import com.venus.backgroundopt.common.entity.message.ForegroundProcTrimMemPolicy
import com.venus.backgroundopt.common.entity.message.GlobalOomScoreEffectiveScopeEnum
import com.venus.backgroundopt.common.entity.message.GlobalOomScorePolicy
import com.venus.backgroundopt.common.entity.preference.OomWorkModePref
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.common.environment.CommonProperties.subProcessDefaultUpgradeSet
import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.common.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.common.preference.PropertyValueWrapper
import com.venus.backgroundopt.common.util.KeyUtils
import com.venus.backgroundopt.common.util.UserUtils
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.log.logInfo
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.util.preference.PreferencesUtil
import com.venus.backgroundopt.xposed.util.preference.PreferencesUtil.prefAll
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
        (prefAll(
            PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
        ) ?: ConcurrentHashMap<String, SubProcessOomPolicy>()).apply {
            subProcessDefaultUpgradeSet.forEach { processKey ->
                if (!this.containsKey(processKey)) {
                    this[processKey] = SubProcessOomPolicy().apply {
                        policyEnum = SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
                    }
                }
            }
        }
    }

    /**
     * 以给定的[userId]和[processName], 从[subProcessOomPolicyMap]中获取[SubProcessOomPolicy]
     */
    @JvmStatic
    fun getSubProcessOomPolicy(userId: Int, processName: String): SubProcessOomPolicy? {
        return subProcessOomPolicyMap[KeyUtils.getProcessKey(userId, processName)]
    }

    /**
     * 从[subProcessOomPolicyMap]中替换[userId]和[processName]指向的值, 并返回旧值
     */
    @JvmStatic
    fun replaceSubProcessOomPolicy(
        subProcessOomPolicy: SubProcessOomPolicy,
        userId: Int = subProcessOomPolicy.userId,
        processName: String = subProcessOomPolicy.processName,
    ): SubProcessOomPolicy? {
        return subProcessOomPolicyMap.replace(
            KeyUtils.getProcessKey(userId, processName),
            subProcessOomPolicy
        )
    }

    /**
     * 从[subProcessOomPolicyMap]中移除[userId]和[processName]指向的值
     */
    @JvmStatic
    fun removeSubProcessOomPolicy(userId: Int, processName: String): SubProcessOomPolicy? {
        return removeSubProcessOomPolicy(KeyUtils.getProcessKey(userId, processName))
    }

    @JvmStatic
    fun removeSubProcessOomPolicy(userId: Int, processNames: Collection<String>) {
        processNames.forEach { processName ->
            removeSubProcessOomPolicy(userId, processName)
        }
    }

    @JvmStatic
    fun removeSubProcessOomPolicy(key: String): SubProcessOomPolicy? {
        return subProcessOomPolicyMap.remove(key)
    }

    @JvmStatic
    fun getUpgradeSubProcessNames(): Set<String> {
        return subProcessOomPolicyMap.values.asSequence()
            .filter { policy->
                policy.policyEnum == SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
            }
            .map { policy->
                policy.processName
            }
            .toSet()
        
    }

    @JvmStatic
    fun isUpgradeSubProcessLevel(userId: Int, processName: String): Boolean {
        val processKey = KeyUtils.getProcessKey(userId, processName)
        return subProcessOomPolicyMap[processKey]?.let {
            when (it.policyEnum) {
                SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS,
                SubProcessOomPolicy.SubProcessOomPolicyEnum.CUSTOM_ADJ,
                    -> true

                else -> false
            }
        } ?: false
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
     * 进程内存回收                                                              *
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
     * app优化策略<[RunningInfo.getAppKey], [AppOptimizePolicy]>
     */
    val appOptimizePolicyMap: MutableMap<String, AppOptimizePolicy> by lazy {
        (prefAll(PreferenceNameConstants.APP_OPTIMIZE_POLICY)
            ?: ConcurrentHashMap<String, AppOptimizePolicy>()).apply {
            logger.info("已配置的App优化策略的数量: ${this.size}")
        }
    }

    /**
     * 根据[userId]和[packageName]查找[AppOptimizePolicy]
     */
    @JvmStatic
    fun getAppOptimizePolicy(userId: Int, packageName: String): AppOptimizePolicy? {
        return appOptimizePolicyMap[RunningInfo.getAppKey(userId, packageName)]
    }

    @JvmStatic
    fun getAppOptimizePolicyByUid(uid: Int, packageName: String): AppOptimizePolicy? {
        val userId = UserUtils.getUserId(uid)
        return getAppOptimizePolicy(userId, packageName)
    }

    /**
     * 从[appOptimizePolicyMap]中移除匹配的[AppOptimizePolicy]
     */
    @JvmStatic
    fun removeAppOptimizePolicy(key: String): AppOptimizePolicy? {
        return appOptimizePolicyMap.remove(key)
    }

    @JvmStatic
    fun removeAppOptimizePolicy(userId: Int, packageName: String): AppOptimizePolicy? {
        return removeAppOptimizePolicy(RunningInfo.getAppKey(userId, packageName))
    }

    @JvmStatic
    fun removeAppOptimizePolicyByUid(uid: Int, packageName: String): AppOptimizePolicy? {
        val userId = UserUtils.getUserId(uid)
        return removeAppOptimizePolicy(userId, packageName)
    }

    /**
     * 以给定的[appOptimizePolicy]替换[appOptimizePolicyMap]中的值
     */
    @JvmStatic
    @JvmOverloads
    fun replaceAppOptimizePolicy(
        appOptimizePolicy: AppOptimizePolicy,
        userId: Int = appOptimizePolicy.userId,
        packageName: String = appOptimizePolicy.packageName,
    ): AppOptimizePolicy? {
        val key = RunningInfo.getAppKey(userId, packageName)
        return appOptimizePolicyMap.replace(key, appOptimizePolicy)
    }

    /**
     * 对[appOptimizePolicyMap]应用 [MutableMap.compute]
     */
    @JvmStatic
    fun appOptimizePolicyMapComputeAction(
        appOptimizePolicy: AppOptimizePolicy,
        block: (String, AppOptimizePolicy?) -> AppOptimizePolicy,
    ) {
        appOptimizePolicyMapComputeAction(
            userId = appOptimizePolicy.userId,
            packageName = appOptimizePolicy.packageName,
            block
        )
    }

    @JvmStatic
    fun appOptimizePolicyMapComputeAction(
        userId: Int,
        packageName: String,
        block: (String, AppOptimizePolicy?) -> AppOptimizePolicy,
    ) {
        appOptimizePolicyMap.compute(RunningInfo.getAppKey(userId, packageName), block)
    }

    /**
     * 根据[userId]和[packageName]计算出[AppOptimizePolicy]
     */
    @JvmStatic
    fun computeAppOptimizePolicy(userId: Int, packageName: String): AppOptimizePolicy {
        return AppOptimizePolicy().apply {
            this.userId = userId
            this.packageName = packageName
        }
    }

    /**
     * 根据[userId]和[packageName]对[appOptimizePolicyMap]使用 [MutableMap.computeIfAbsent]
     */
    @JvmStatic
    fun computeAppOptimizePolicyInMap(userId: Int, packageName: String): AppOptimizePolicy {
        return appOptimizePolicyMap.computeIfAbsent(RunningInfo.getAppKey(userId, packageName)) {
            computeAppOptimizePolicy(userId = userId, packageName = packageName)
        }
    }

    @JvmStatic
    fun computeAppOptimizePolicyInMapByUid(uid: Int, packageName: String): AppOptimizePolicy {
        val userId = UserUtils.getUserId(uid)
        return computeAppOptimizePolicyInMap(userId, packageName)
    }

    @JvmStatic
    @Deprecated(
        message = "AppOptimizePolicy.shouldHandleAdjUiState已被取代",
        replaceWith = ReplaceWith("mainProcessAdjManagePolicy")
    )
    fun setShouldHandleAdjUiState(
        userId: Int,
        packageName: String,
        shouldHandleAdjUiState: Boolean,
    ) {
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
    fun printKeepMainProcessAliveHasActivityInfo(isEnabled: Boolean) {
        printPreferenceActiveState(
            isEnabled = isEnabled,
            description = "拥有界面的app的主进程临时保活"
        )
    }

    @JvmStatic
    fun isEnableKeepMainProcessAliveHasActivity(): Boolean =
        keepMainProcessAliveHasActivityPolicy.value

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
    @JvmStatic
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