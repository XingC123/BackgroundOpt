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

import android.os.Build
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManager
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getObjectFieldValue

/**
 * @author XingC
 * @date 2024/2/1
 */
class ActivityManagerServiceHookNew(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    val helpKillProcessesForRemovedTask =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

    override fun hook() {
        ClassConstants.ActivityManagerService.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.setMemFactorOverride,
            paramTypes = arrayOf(Int::class.java)
        ) { it.result = null }

        ClassConstants.LocalService.beforeHook(
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
}