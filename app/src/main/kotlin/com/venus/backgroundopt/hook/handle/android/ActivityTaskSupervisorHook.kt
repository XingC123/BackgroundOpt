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
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue

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
            methodName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                MethodConstants.cleanUpRemovedTask
            } else {
                MethodConstants.cleanUpRemovedTaskLocked
            },
            hookAllMethod = true,
        ) { param ->
            val taskInstance = param.args[0] as Any

            val packageName =
                (taskInstance.getObjectFieldValue(fieldName = FieldConstants.intent) as? Intent)?.let { intent ->
                    intent.`package` ?: intent.component?.packageName
                } ?: return@beforeHook
            val userId = taskInstance.getIntFieldValue(fieldName = FieldConstants.mUserId)

            val appInfo = runningInfo.getRunningAppInfo(userId, packageName) ?: return@beforeHook
            val appOptimizePolicy = CommonProperties.appOptimizePolicyMap[appInfo.packageName]
            val globalOomScorePolicy = CommonProperties.globalOomScorePolicy.value

            if (appOptimizePolicy != null) {
                if (appOptimizePolicy.enableCustomMainProcessOomScore && appOptimizePolicy.customMainProcessOomScore <= ProcessList.PERSISTENT_PROC_ADJ) {
                    runningInfo.forceStopRunningApp(appInfo)
                    removeRecentTaskLog(userId = userId, packageName = packageName)
                }
            } else if (globalOomScorePolicy.enabled && globalOomScorePolicy.customGlobalOomScore <= ProcessList.PERSISTENT_PROC_ADJ) {
                runningInfo.forceStopRunningApp(appInfo)
                removeRecentTaskLog(userId = userId, packageName = packageName)
            }
        }
    }

    private fun removeRecentTaskLog(userId: Int, packageName: String) {
        logger.info("移除最近任务: userId: ${userId}, packageName: ${packageName}")
    }
}