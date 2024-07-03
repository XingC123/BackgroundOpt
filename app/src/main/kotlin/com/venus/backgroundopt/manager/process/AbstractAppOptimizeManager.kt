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

package com.venus.backgroundopt.manager.process

import android.os.SystemClock
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.runCatchThrowable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author XingC
 * @date 2023/10/14
 */
abstract class AbstractAppOptimizeManager(val appOptimizeEnum: AppOptimizeEnum) {
    abstract fun getExecutor(): Executor

    open fun isNecessaryToOptimizeProcess(processRecord: ProcessRecord): Boolean {
        val isSafeOomAdj = processRecord.oomAdjScore >= 0
        return if (isSafeOomAdj) {
            if (processRecord.mainProcess) {
                processRecord.isNecessaryToOptimize()
            } else {
                true
            }
        } else {
            false
        }
    }

    open fun getBackgroundFirstTaskDelay(): Long = 0

    open fun getBackgroundFirstTaskDelayTimeUnit(): TimeUnit = TimeUnit.SECONDS

    val backgroundFirstTaskMap: MutableMap<ProcessRecord, ScheduledFuture<*>> =
        ConcurrentHashMap(4)

    open fun addBackgroundFirstTask(
        processRecord: ProcessRecord,
        block: (() -> Unit)? = null,
    ) {
        backgroundFirstTaskMap.computeIfAbsent(processRecord) { _ ->
            (getExecutor() as ScheduledThreadPoolExecutor).schedule(
                {
                    runCatchThrowable(finallyBlock = {
                        backgroundFirstTaskMap.remove(processRecord)
                    }) {
                        block?.let { it() }
                    }
                },
                getBackgroundFirstTaskDelay(),
                getBackgroundFirstTaskDelayTimeUnit()
            )
        }
    }

    open fun removeBackgroundFirstTask(
        processRecord: ProcessRecord,
        block: ((ScheduledFuture<*>) -> Unit)? = null,
    ) {
        backgroundFirstTaskMap.remove(processRecord)?.let { scheduledFuture ->
            scheduledFuture.cancel(true)
            block?.let { it(scheduledFuture) }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 上一次进程执行结果                                                          *
     *                                                                         *
     **************************************************************************/
    open fun getProcessingResultIfAbsent(): ProcessingResult = ProcessingResult()

    inline fun updateProcessLastProcessingResult(
        processRecord: ProcessRecord,
        appOptEnum: AppOptimizeEnum = appOptimizeEnum,
        block: (ProcessingResult) -> Unit
    ) {
        val processingResult = processRecord.initLastProcessingResultIfAbsent(
            appOptimizeEnum = appOptEnum,
            processingResultSupplier = ::getProcessingResultIfAbsent
        )

        processingResult.lastProcessingTime = SystemClock.uptimeMillis()
        block(processingResult)
    }

    fun removeProcessLastProcessingResult(
        processRecord: ProcessRecord,
        appOptEnum: AppOptimizeEnum = appOptimizeEnum,
    ) {
        processRecord.lastProcessingResultMap.remove(appOptEnum)
    }

    fun removeProcessLastProcessingResultFromSet(set: Set<ProcessRecord>) {
        set.forEach { process ->
            removeProcessLastProcessingResult(process)
        }
    }

    enum class AppOptimizeEnum {
        FOREGROUND_TRIM_MEM,
        BACKGROUND_TRIM_MEM,
        BACKGROUND_GC,
        PROCESS_COMPACT,
        PROCESS_MEM_TRIM,
    }
}