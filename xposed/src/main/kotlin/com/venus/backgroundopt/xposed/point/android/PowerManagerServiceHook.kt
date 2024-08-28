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

import android.os.IBinder
import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.power.PowerManagerService.PowerManagerServiceHelper
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.afterConstructorHook
import com.venus.backgroundopt.xposed.util.afterHook
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2024/3/9
 */
class PowerManagerServiceHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    val wakeLockMap = ConcurrentHashMap<IBinder, ProcessRecord>(8)

    override fun hook() {
        ClassConstants.PowerManagerService.afterConstructorHook(
            classLoader = classLoader,
            hookAllMethod = true
        ) { param ->
            runningInfo.powerManagerService = PowerManagerServiceHelper.instanceCreator(
                param.thisObject
            )
        }

        val pidParamIndex = if (OsUtils.isSOrHigher) 8 else 7
        val flagsParamIndex = if (OsUtils.isSOrHigher) 2 else 1

        ClassConstants.PowerManagerService.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.acquireWakeLockInternal,
            /*paramTypes = arrayOf(
                IBinder::class.java,    /* lock */
                Int::class.java,        /* displayId */
                Int::class.java,        /* flags */
                String::class.java,     /* tag */
                String::class.java,     /* packageName */
                WorkSource::class.java, /* ws */
                String::class.java,     /* historyTag */
                Int::class.java,        /* uid */
                Int::class.java,        /* pid */
                ClassConstants.IWakeLockCallback,  /* callback */
            ),*/
            hookAllMethod = true,
        ) { param ->
            val pid = param.args[pidParamIndex] as Int
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@afterHook
            // 主进程暂无需关注唤醒锁
            if (processRecord.mainProcess) {
                return@afterHook
            }
            val lock = param.args[0] as IBinder
            wakeLockMap.computeIfAbsent(lock) { processRecord.also { it.incrementWakeLockCount() } }

            if (BuildConfig.DEBUG) {
                val flags = param.args[flagsParamIndex] as Int
                logger.debug("pid: ${pid}, packageName: [${processRecord.packageName}], processName: [${processRecord.processName}] 正在请求唤醒锁, tag: ${param.args[3]}, flags: ${flags}")
            }
        }

        ClassConstants.PowerManagerService.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.releaseWakeLockInternal,
            hookAllMethod = true
        ) { param ->
            val lock = param.args[0] as IBinder
            wakeLockMap.remove(lock)?.let { processRecord ->
                processRecord.decrementWakeLockCount()

                if (BuildConfig.DEBUG) {
                    logger.debug("pid: ${processRecord.pid}, packageName: [${processRecord.packageName}], processName: [${processRecord.processName}] 释放唤醒锁")
                }
            }
        }
    }
}