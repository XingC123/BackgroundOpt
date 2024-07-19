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

package com.venus.backgroundopt.xposed.manager.message.handle

import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy
import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy.MainProcessAdjManagePolicy
import com.venus.backgroundopt.common.entity.message.AppOptimizePolicyMessage
import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.util.parseObjectFromJsonObject
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessList
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.message.MessageHandler
import com.venus.backgroundopt.xposed.manager.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * app优化策略消息处理器
 *
 * @author XingC
 * @date 2023/11/5
 */
class AppOptimizePolicyMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?,
    ) {
        createResponse<AppOptimizePolicyMessage>(param, value, setJsonData = true) { message ->
            var returnValue: Any? = null

            val appOptimizePolicyMap = HookCommonProperties.appOptimizePolicyMap
            val packageName = message.packageName

            when (message.messageType) {
                AppOptimizePolicyMessage.MSG_NONE -> {}

                AppOptimizePolicyMessage.MSG_CREATE_OR_GET -> {
                    returnValue = appOptimizePolicyMap.computeIfAbsent(packageName) {
                        AppOptimizePolicy().apply {
                            this.packageName = packageName
                        }
                    }.apply {
                        initMainProcessAdjManagePolicyUiText(this)
                    }
                }

                AppOptimizePolicyMessage.MSG_SAVE -> {
                    val appOptimizePolicy =
                        message.value.parseObjectFromJsonObject<AppOptimizePolicy>()!!
                    val old = appOptimizePolicyMap[appOptimizePolicy.packageName]

                    appOptimizePolicyMap[appOptimizePolicy.packageName] = appOptimizePolicy

                    runningInfo.runningAppInfos.asSequence()
                        .filter { appInfo -> appInfo.packageName == appOptimizePolicy.packageName }
                        .forEach { appInfo -> appInfo.setAdjHandleFunction(appOptimizePolicy) }

                    if (old?.enableCustomMainProcessOomScore != appOptimizePolicy.enableCustomMainProcessOomScore) {
                        ProcessRecord.resetAdjHandleType(
                            packageName = packageName
                        )
                    }

                    initMainProcessAdjManagePolicyUiText(appOptimizePolicy)
                    returnValue = appOptimizePolicy
                }

                else -> {}
            }
            returnValue
        }
    }

    private fun initMainProcessAdjManagePolicyUiText(appOptimizePolicy: AppOptimizePolicy) {
        appOptimizePolicy.defaultMainProcessAdjManagePolicyUiText =
            if (appOptimizePolicy.mainProcessAdjManagePolicy == MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_DEFAULT) {
                PreferenceDefaultValue.mainProcessAdjManagePolicy.uiText
            } else {
                appOptimizePolicy.mainProcessAdjManagePolicy.uiText
            }
    }
}

fun AppOptimizePolicy?.isEnabledCustomMainProcessAdj(): Boolean {
    return this?.enableCustomMainProcessOomScore == true
}

/**
 * 获取自定义的主进程oom分数
 * @receiver AppOptimizePolicyMessageHandler.AppOptimizePolicy? app优化策略
 * @return Int? 自定义主进程oom分数
 */
fun AppOptimizePolicy?.isCustomMainProcessAdjValid(): Boolean {
    if (this?.enableCustomMainProcessOomScore != true) {
        return false
    }
    return ProcessList.isValidAdj(customMainProcessFgAdj)
            || ProcessList.isValidAdj(customMainProcessOomScore)
}

private fun AppOptimizePolicy.getCustomMainProcessAdj(adj: Int): Int? {
    return if (enableCustomMainProcessOomScore && ProcessList.isValidAdj(adj)) adj else null
}

fun AppOptimizePolicy?.getCustomMainProcessFgAdj(): Int? {
    return this?.getCustomMainProcessAdj(customMainProcessFgAdj)
}

fun AppOptimizePolicy?.getCustomMainProcessBgAdj(): Int? {
    return this?.getCustomMainProcessAdj(customMainProcessOomScore)
}

fun AppOptimizePolicy.getCustomMainProcessFgAdjFromNonNull(): Int? {
    return getCustomMainProcessAdj(customMainProcessFgAdj)
}

fun AppOptimizePolicy.getCustomMainProcessBgAdjFromNonNull(): Int? {
    return getCustomMainProcessAdj(customMainProcessOomScore)
}