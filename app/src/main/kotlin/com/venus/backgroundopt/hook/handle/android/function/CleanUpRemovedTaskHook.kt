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

package com.venus.backgroundopt.hook.handle.android.function

import android.content.Intent
import com.venus.backgroundopt.annotation.FunctionHook
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.hook.HookCommonProperties
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.HookTagConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManager
import com.venus.backgroundopt.hook.handle.android.entity.ApplicationExitInfo
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.SystemUtils
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2024/5/23
 */
@FunctionHook("划卡杀后台")
class CleanUpRemovedTaskHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo
) : IHook(classLoader, runningInfo) {
    override fun getHookTag(): String = HookTagConstants.CleanUpRemovedTask

    init {
        // 注册监听
        HookCommonProperties.enableKillAfterRemoveTask.addListener(
            getHookTag()
        ) { _, newValue ->
            if (newValue) {
                doHook()
            } else {
                unhook(getHookTag())
            }
        }
    }

    override fun hook() {
        // 检查是否开启了"划卡杀后台"
        if (!HookCommonProperties.enableKillAfterRemoveTask.value) {
            return
        }

        doHook()
    }

    val helpKillProcessesForRemovedTask = if (SystemUtils.isUOrHigher) {
        { processStateRecord: Any ->
            // a14的ActivityManagerService.killProcessesForRemovedTask会先判断此属性
            processStateRecord.callMethod(
                methodName = "setSetProcState",
                ActivityManager.PROCESS_STATE_TRANSIENT_BACKGROUND
            )
        }
    } else {
        { processStateRecord: Any ->
            // a12的ActivityManagerService.killProcessesForRemovedTask会先判断此属性
            processStateRecord.callMethod(
                methodName = "setSetSchedGroup",
                ProcessList.SCHED_GROUP_BACKGROUND
            )
        }
    }

    // 移除最近任务卡片需要的方法的名字
    private val methodNameForCleanUpRemovedTask: String
        get() = if (SystemUtils.isUOrHigher) {
            MethodConstants.cleanUpRemovedTask
        } else {
            MethodConstants.cleanUpRemovedTaskLocked
        }

    override fun doHook() {
        ClassConstants.ActivityTaskSupervisor.beforeHook(
            classLoader = classLoader,
            methodName = methodNameForCleanUpRemovedTask,
            tag = getHookTag(),
            hookAllMethod = true,
        ) { param ->
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
            val appOptimizePolicy = HookCommonProperties.appOptimizePolicyMap[appInfo.packageName]
            val globalOomScorePolicy = HookCommonProperties.globalOomScorePolicy.value

            if (appOptimizePolicy?.enableCustomMainProcessOomScore == true) {
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
            methodName = methodNameForCleanUpRemovedTask,
            hookAllMethod = true,
        ) {
            killProcessesForRemovedTask()
        }

        /*
         * 若shouldKillProcessForRemovedTask返回false, 则检查是否含有前台服务
         */
        ClassConstants.WindowProcessController.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.shouldKillProcessForRemovedTask,
            hookAllMethod = true
        ) { param ->
            val shouldKillProc = param.result as Boolean
            if (!shouldKillProc) {
                return@afterHook
            }

            val windowProcessController = param.thisObject
            val hasForegroundServices = windowProcessController.callMethod(
                methodName = MethodConstants.hasForegroundServices
            ) as Boolean
            if (hasForegroundServices) {
                // 加入待移除列表
                removedTaskWindowProcessControllerSet.add(windowProcessController)
            }
        }

        ClassConstants.LocalService.beforeHook(
            enable = false,
            classLoader = classLoader,
            methodName = MethodConstants.killProcessesForRemovedTask,
            hookAllMethod = true
        ) { param ->
            // 获取要查杀的进程列表
            val list = param.args[0] as List<*>

            list.forEach { windowProcessController ->
                windowProcessController?.let {
                    windowProcessController.getObjectFieldValue(
                        fieldName = FieldConstants.mOwner
                    )?.let { processRecord ->
                        processRecord.getObjectFieldValue(
                            fieldName = FieldConstants.mReceivers
                        )?.let { mReceivers ->
                            val numberOfCurReceivers = mReceivers.callMethod(
                                methodName = MethodConstants.numberOfCurReceivers
                            ) as Int
                            if (numberOfCurReceivers == 0) {
                                val processStateRecord = processRecord.getObjectFieldValue(
                                    fieldName = FieldConstants.mState
                                )!!
                                helpKillProcessesForRemovedTask(processStateRecord)
                            }
                        }
                    }
                }
            }
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

        @JvmStatic
        fun killProcessesForRemovedTask() {
            val set = removedTaskWindowProcessControllerSet
            set.forEach { windowProcessController ->
                windowProcessController.getObjectFieldValue(
                    fieldName = FieldConstants.mOwner
                )?.let inner@{ processRecord ->
                    val mReceivers = processRecord.getObjectFieldValue(
                        fieldName = FieldConstants.mReceivers
                    ) ?: return@inner
                    val numberOfCurReceivers = mReceivers.callMethod(
                        methodName = MethodConstants.numberOfCurReceivers
                    )
                    // 这里舍弃了安卓对windowProcessController的另一个判断
                    if (numberOfCurReceivers == 0) {
                        processRecord.callMethod(
                            methodName = MethodConstants.killLocked,
                            "remove task",
                            ApplicationExitInfo.REASON_USER_REQUESTED,
                            ApplicationExitInfo.SUBREASON_REMOVE_TASK,
                            true
                        )
                    } else {
                        processRecord.callMethod(
                            methodName = MethodConstants.setWaitingToKill,
                            "remove task"
                        )
                    }
                }
                set.remove(windowProcessController)
            }
        }
    }
}