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

package com.venus.backgroundopt.hook.handle.android

import android.content.Intent
import android.os.Build
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2024/3/8
 */
class ActivityTaskSupervisorHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.ActivityTaskSupervisor.beforeHook(
            classLoader = classLoader,
            methodName = getMethodNameForCleanUpRemovedTask(),
            hookAllMethod = true,
        ) { param ->
            if (!CommonProperties.enableKillAfterRemoveTask.value) {
                return@beforeHook
            }

            var killProcess = param.args[1] as Boolean
            // 如果原本就是true, 我们就不需要管他
            if (killProcess) {
                return@beforeHook
            }

            val taskInstance = param.args[0] as Any
            val packageName =
                (taskInstance.getObjectFieldValue(fieldName = FieldConstants.intent) as? Intent)?.let { intent ->
                    intent.`package` ?: intent.component?.packageName
                } ?: return@beforeHook
            val userId = taskInstance.getIntFieldValue(fieldName = FieldConstants.mUserId)
            val appInfo = runningInfo.getRunningAppInfo(userId, packageName) ?: return@beforeHook
            val appOptimizePolicy = CommonProperties.appOptimizePolicyMap[appInfo.packageName]
            val globalOomScorePolicy = CommonProperties.globalOomScorePolicy.value

            if (appOptimizePolicy != null && appOptimizePolicy.enableCustomMainProcessOomScore) {
                // 自定义主进程会优先于全局oom。
                // 因此此处只要不需要处理, 那会由系统解决
                if (appOptimizePolicy.customMainProcessOomScore <= ProcessList.PERSISTENT_PROC_ADJ) {
                    removeRecentTaskLog(userId = userId, packageName = packageName)
                    // runningInfo.forceStopRunningApp(appInfo)
                    killProcess = true
                }
            } else if (globalOomScorePolicy.enabled && globalOomScorePolicy.customGlobalOomScore <= ProcessList.PERSISTENT_PROC_ADJ) {
                removeRecentTaskLog(userId = userId, packageName = packageName)
                // runningInfo.forceStopRunningApp(appInfo)
                killProcess = true
            }

            param.args[1] = killProcess
        }.afterHook(
            classLoader = classLoader,
            methodName = getMethodNameForCleanUpRemovedTask(),
            hookAllMethod = true,
        ) {
            ActivityManagerServiceHookNew.killProcessesForRemovedTask()
        }
    }

    /**
     * 获取hook移除最近任务卡片需要的方法的名字
     * @return String
     */
    private fun getMethodNameForCleanUpRemovedTask(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            MethodConstants.cleanUpRemovedTask
        } else {
            MethodConstants.cleanUpRemovedTaskLocked
        }
    }

    private fun removeRecentTaskLog(userId: Int, packageName: String) {
        logger.info("移除最近任务, 来自: userId: ${userId}, packageName: ${packageName}")
    }

    companion object {
        /**
         * 要被移除的[ClassConstants.WindowProcessController]
         */
        @JvmField
        val removedTaskWindowProcessControllerSet: MutableSet<Any> = Collections.newSetFromMap(
            ConcurrentHashMap()
        )
    }
}