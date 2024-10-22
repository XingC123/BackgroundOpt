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

package com.venus.backgroundopt.xposed.manager.process.memory

import android.os.SystemClock
import com.venus.backgroundopt.common.entity.preference.OomWorkModePref
import com.venus.backgroundopt.common.util.concurrent.ConcurrentUtils
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.core.AppGroupEnum
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.CachedAppOptimizer
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessList
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.process.AppOptimizeEnum.PROCESS_COMPACT
import com.venus.backgroundopt.xposed.manager.process.ProcessCompactResultCode
import com.venus.backgroundopt.xposed.manager.process.ProcessingResult
import com.venus.backgroundopt.xposed.manager.process.memory.ProcessCompactEnum.FULL
import com.venus.backgroundopt.xposed.manager.process.memory.ProcessCompactEnum.NONE
import com.venus.backgroundopt.xposed.manager.process.memory.ProcessCompactEnum.SOME
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 应用内存压缩管理器
 *
 * @author XingC
 * @date 2023/8/8
 */
class AppCompactManager2(
    private val cachedAppOptimizer: CachedAppOptimizer,
    private val runningInfo: RunningInfo,
) : AbstractAppOptimizeManager(PROCESS_COMPACT), ILogger {
    // App压缩处理线程池
    private val executor = ExecutorUtils.newScheduleThreadPool(
        coreSize = ConcurrentUtils.computeIntensiveTaskThreadCount,
        factoryName = "appCompactPool",
        removeOnCancelPolicy = true
    )

    private val isSpecialOomWorkMode = HookCommonProperties.oomWorkModePref.oomMode.run {
        this == OomWorkModePref.MODE_STRICT || this == OomWorkModePref.MODE_NEGATIVE
    }

    override fun getExecutor(): Executor = executor

    override fun getProcessingResultIfAbsent(): ProcessingResult {
        return ProcessCompactProcessingResult()
    }

    private val compactProcessMap: MutableMap<ProcessRecord, ScheduledFuture<*>> =
        ConcurrentHashMap()

    override fun isNecessaryToOptimizeProcess(processRecord: ProcessRecord): Boolean {
        return processRecord.oomAdjScore >= 0 && processRecord.appInfo.appGroupEnum != AppGroupEnum.ACTIVE
    }

    fun compactProcess(
        processRecord: ProcessRecord,
        lastOomScoreAdj: Int,
        curOomScoreAdj: Int,
        oomAdjustLevel: Int,
    ) {
        if (processRecord.appInfo.appGroupEnum != AppGroupEnum.IDLE) {
            return
        }
        if (lastOomScoreAdj == curOomScoreAdj) {
            return
        }

        compactProcessMap.compute(processRecord) { _, lastScheduledFuture ->
            lastScheduledFuture?.cancel(true)
            executor.schedule(
                {
                    runCatchThrowable(catchBlock = {
                        logger.error("压缩进程任务出错", it)
                    }) {
                        compactProcessImpl(
                            processRecord = processRecord,
                            lastOomScoreAdj = lastOomScoreAdj,
                            curOomScoreAdj = curOomScoreAdj,
                            oomAdjustLevel = oomAdjustLevel,
                        )
                    }
                },
                /*cachedAppOptimizer.mFreezerDebounceTimeout*/
                COMPACT_TASK_DELAY,
                TimeUnit.MILLISECONDS
            )
        }
    }

    private fun compactProcessImpl(
        processRecord: ProcessRecord,
        lastOomScoreAdj: Int,
        curOomScoreAdj: Int,
        oomAdjustLevel: Int,
    ) {
        // app处于前台时不进行压缩
        if (processRecord.appInfo.appGroupEnum == AppGroupEnum.ACTIVE) {
            return
        }

        // 是否进行了压缩
        var doCompact = false
        var processCompactResultCode = ProcessCompactResultCode.doNothing
        var processCompactEnum = NONE
        val processingResult = processRecord.initLastProcessingResultIfAbsent(
            appOptimizeEnum = appOptimizeEnum,
            processingResultSupplier = ::getProcessingResultIfAbsent
        ) as ProcessCompactProcessingResult
        val currentTimeMillis = SystemClock.uptimeMillis()
        val lastProcessCompatEnum = processingResult.processCompactEnum
        var compactAction = Int.MIN_VALUE
        /*var compactReason = "OOM_SCORE"
        var compactBecauseProcAllow = false*/

        /*if (isSpecialOomWorkMode) {
            if (processRecord.isAllowedCompact(currentTimeMillis)) {
                doCompact = true
                processCompactEnum = ProcessCompactEnum.FULL
                compactAction = CachedAppOptimizer.COMPACT_ACTION_FULL
            }
        } else {*/
        val timeDifference = currentTimeMillis - processingResult.lastProcessingTime
        if (ProcessList.PERCEPTIBLE_APP_ADJ in lastOomScoreAdj..curOomScoreAdj && curOomScoreAdj <= ProcessList.SERVICE_B_ADJ) {
            if ((lastProcessCompatEnum == SOME && timeDifference < cachedAppOptimizer.mCompactThrottleSomeSome)
                || (lastProcessCompatEnum == FULL && timeDifference < cachedAppOptimizer.mCompactThrottleSomeFull)
            ) {
                // do nothing
            } else {
                doCompact = true
                processCompactEnum = SOME
                compactAction = CachedAppOptimizer.COMPACT_ACTION_FILE
            }
        } else if (ProcessList.CACHED_APP_MIN_ADJ in lastOomScoreAdj..curOomScoreAdj
            || processRecord.isAllowedCompact(
                time = currentTimeMillis
            )/*.also { compactBecauseProcAllow = it }*/
        ) {
            if ((lastProcessCompatEnum == SOME && timeDifference < cachedAppOptimizer.mCompactThrottleFullSome)
                || (lastProcessCompatEnum == FULL && timeDifference < cachedAppOptimizer.mCompactThrottleFullFull)
            ) {
                // do nothing
            } else {
                /*if (compactBecauseProcAllow) {
                    compactReason = "从未压缩或超时"
                }*/
                doCompact = true
                processCompactEnum = FULL
                compactAction = CachedAppOptimizer.COMPACT_ACTION_FULL
            }
        }
        // }

        if (doCompact) {
            // 检查进程是否需要进行优化
            if (!isNecessaryToOptimizeProcess(processRecord)) {
                updateProcessLastProcessingResult(processRecord) {
                    it.lastProcessingCode = ProcessCompactResultCode.unNecessary
                }

                /*if (BuildConfig.DEBUG) {
                    logger.debug("uid: ${processRecord.uid}, pid: ${processRecord.pid}, 主进程: ${processRecord.mainProcess}, 包名: ${processRecord.packageName}不需要压缩")
                }*/
                return
            }

            processCompactResultCode = compactProcess(processRecord.pid, compactAction)
            updateProcessLastProcessingResult(processRecord = processRecord) {
                processingResult.lastProcessingCode = processCompactResultCode
                processingResult.processCompactEnum = processCompactEnum
            }
        }

        /*if (BuildConfig.DEBUG) {
            when (processCompactResultCode) {
                ProcessCompactResultCode.success -> {
                    logger.debug(
                        "uid: ${processRecord.uid}, pid: ${processRecord.pid}, 主进程: ${processRecord.mainProcess}, packageName: ${processRecord.packageName} >>> "
                                + "因[${compactReason}] 而内存压缩"
                    )
                }

                ProcessCompactResultCode.doNothing -> {
                    logger.debug(
                        "uid: ${processRecord.uid}, pid: ${processRecord.pid}, 主进程: ${processRecord.mainProcess}, packageName: ${processRecord.packageName} >>> "
                                + "未执行内存压缩"
                    )
                }

                else -> {
                    logger.warn(
                        "uid: ${processRecord.uid}, pid: ${processRecord.pid}, 主进程: ${processRecord.mainProcess}, packageName: ${processRecord.packageName} >>> " +
                                "因[${compactReason}] 而内存压缩: 发生异常!压缩执行失败或进程已停止"
                    )
                }
            }
        }*/
    }

    private fun compactProcess(
        pid: Int,
        compactAction: Int,
    ): Int {
        return if (cachedAppOptimizer.compactProcess(pid, compactAction)) {
            ProcessCompactResultCode.success
        } else {
            ProcessCompactResultCode.problem
        }
    }

    companion object {
        const val COMPACT_TASK_DELAY = 10L * 1000
    }
}

class ProcessCompactProcessingResult : ProcessingResult() {
    var processCompactEnum = NONE
}

enum class ProcessCompactEnum {
    NONE,
    SOME,
    FULL,
}