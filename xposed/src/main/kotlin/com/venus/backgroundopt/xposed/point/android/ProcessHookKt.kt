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

package com.venus.backgroundopt.xposed.point.android

import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.AppGroupEnum
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.android.os.Process
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.MethodHook
import com.venus.backgroundopt.xposed.manager.process.ProcessManager
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/19
 */
class ProcessHookKt(
    classLoader: ClassLoader,
    hookInfo: RunningInfo,
) : MethodHook(classLoader, hookInfo) {
    companion object {
        // 在此处的App内存状态将不会允许系统设置ProcessGroup
        val ignoreSetProcessGroupAppGroups = arrayOf(
            AppGroupEnum.IDLE,
            AppGroupEnum.TMP,
            AppGroupEnum.ACTIVE
        )
    }

    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
//            HookPoint(
//                ClassConstants.Process,
//                MethodConstants.killProcessGroup,
//                arrayOf(
//                    beforeHookAction {
//                        handleKillApp(it)
//                    }
//                ),
//                Int::class.java,
//                Int::class.java
//            ),
            /*HookPoint(
                ClassConstants.Process,
                MethodConstants.setProcessGroup,
                arrayOf(
                    beforeHookAction { handleSetProcessGroup(it) }
                ),
                Int::class.javaPrimitiveType,   // pid
                Int::class.javaPrimitiveType    // group
            ),*/
        )
    }

    @Deprecated("执行完毕后有可能会进入ProcessListHook.handleSetOomAdj导致再次新建")
    private fun handleKillApp(param: MethodHookParam) {
        val pid = param.args[1] as Int

        runningInfo.removeProcess(pid)
    }

    @Deprecated("实际实施起来非常复杂")
    private fun handleSetProcessGroup(param: MethodHookParam) {
        val pid = param.args[0] as Int
        val group = param.args[1] as Int

        if (group > Process.THREAD_GROUP_RESTRICTED) {  //若是模块控制的行为, 则直接处理
            //若是模块控制的行为, 则直接处理
            param.args[1] = group - ProcessManager.THREAD_GROUP_LEVEL_OFFSET
            if (BuildConfig.DEBUG) {
                logger.debug("pid: ${pid}设置ProcessGroup >>> ${param.args[1]}")
            }
        } else {
            val processRecord = runningInfo.getRunningProcess(pid) ?: return
            val uid = processRecord.uid
            val appInfo = processRecord.appInfo

            // 模块接管此处行为
            if (appInfo.appGroupEnum in ignoreSetProcessGroupAppGroups) {
                param.result = null
            }
        }
    }
}