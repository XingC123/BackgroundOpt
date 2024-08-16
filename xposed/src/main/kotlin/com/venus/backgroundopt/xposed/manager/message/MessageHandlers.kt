/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.xposed.manager.message

import com.venus.backgroundopt.common.util.log.logDebug
import com.venus.backgroundopt.common.util.message.Message
import com.venus.backgroundopt.common.util.message.MessageKeyConstants
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.manager.message.handle.AppCompactListMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.AppOptimizePolicyMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.AppWebviewProcessProtectMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.AutoStopCompactTaskMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.BackgroundTasksMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.EnableForegroundProcTrimMemPolicyHandler
import com.venus.backgroundopt.xposed.manager.message.handle.ForegroundProcTrimMemPolicyHandler
import com.venus.backgroundopt.xposed.manager.message.handle.GetInstalledPackagesMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.GetManagedAdjDefaultAppsMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.GetTrimMemoryOptThresholdMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.GlobalOomScoreEffectiveScopeMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.GlobalOomScoreMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.GlobalOomScoreValueMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.HomePageModuleInfoMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.KeepMainProcessAliveHasActivityMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.KillAfterRemoveTaskMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.ModuleRunningMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.ProcessRunningInfoMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.ResetAppConfigurationMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.RunningAppInfoMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.RunningProcessListMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.SimpleLmkMessageHandler
import com.venus.backgroundopt.xposed.manager.message.handle.SubProcessOomConfigChangeMessageHandler
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2024/8/16
 */
fun ModuleMessageHandler.handleMessage(
    message: Message,
    methodHookParam: MethodHookParam,
) {
    if (BuildConfig.DEBUG) {
        logDebug(
            logStr = "模块进程接收的数据为: $message"
        )
    }

    // 已注册的消息处理器
    val messageHandler = when (message.key) {
        MessageKeyConstants.getRunningAppInfo -> RunningAppInfoMessageHandler
        MessageKeyConstants.getBackgroundTasks -> BackgroundTasksMessageHandler
        MessageKeyConstants.getAppCompactList -> AppCompactListMessageHandler
        MessageKeyConstants.subProcessOomConfigChange -> SubProcessOomConfigChangeMessageHandler
        MessageKeyConstants.getInstalledApps -> GetInstalledPackagesMessageHandler
        MessageKeyConstants.autoStopCompactTask -> AutoStopCompactTaskMessageHandler
        MessageKeyConstants.enableForegroundProcTrimMemPolicy -> EnableForegroundProcTrimMemPolicyHandler
        MessageKeyConstants.foregroundProcTrimMemPolicy -> ForegroundProcTrimMemPolicyHandler
        MessageKeyConstants.appOptimizePolicy -> AppOptimizePolicyMessageHandler
        MessageKeyConstants.appWebviewProcessProtect -> AppWebviewProcessProtectMessageHandler
        MessageKeyConstants.enableSimpleLmk -> SimpleLmkMessageHandler
        MessageKeyConstants.enableGlobalOomScore -> GlobalOomScoreMessageHandler
        MessageKeyConstants.globalOomScoreEffectiveScope -> GlobalOomScoreEffectiveScopeMessageHandler
        MessageKeyConstants.globalOomScoreValue -> GlobalOomScoreValueMessageHandler
        MessageKeyConstants.getTrimMemoryOptThreshold -> GetTrimMemoryOptThresholdMessageHandler
        MessageKeyConstants.getHomePageModuleInfo -> HomePageModuleInfoMessageHandler
        MessageKeyConstants.killAfterRemoveTask -> KillAfterRemoveTaskMessageHandler
        MessageKeyConstants.moduleRunning -> ModuleRunningMessageHandler
        MessageKeyConstants.getManagedAdjDefaultApps -> GetManagedAdjDefaultAppsMessageHandler
        MessageKeyConstants.KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY -> KeepMainProcessAliveHasActivityMessageHandler
        MessageKeyConstants.getProcessRunningInfo -> ProcessRunningInfoMessageHandler
        MessageKeyConstants.RUNNING_PROCESS_LIST -> RunningProcessListMessageHandler
        MessageKeyConstants.RESET_APP_CONFIGURATION -> ResetAppConfigurationMessageHandler
        else -> null
    }

    messageHandler?.handle(
        runningInfo = runningInfo,
        param = methodHookParam,
        value = message.value.toString()
    )
}