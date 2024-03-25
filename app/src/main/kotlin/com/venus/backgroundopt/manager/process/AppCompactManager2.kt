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
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.entity.preference.OomWorkModePref
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.runCatchThrowable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 应用内存压缩管理器
 *
 * @author XingC
 * @date 2023/8/8
 */
class AppCompactManager2(
    private val cachedAppOptimizer: CachedAppOptimizer,
    private val runningInfo: RunningInfo
) : AbstractAppOptimizeManager(AppOptimizeEnum.PROCESS_COMPACT), ILogger {
    // App压缩处理线程池
    private val executor = ScheduledThreadPoolExecutor(2).apply {
        removeOnCancelPolicy = true
    }

    private val isSpecialOomWorkMode = CommonProperties.oomWorkModePref.oomMode.run {
        this == OomWorkModePref.MODE_STRICT || this == OomWorkModePref.MODE_NEGATIVE
    }

    override fun getExecutor(): Executor = executor

    override fun getProcessingResultIfAbsent(): ProcessingResult {
        return ProcessCompactProcessingResult()
    }

    private val compactProcessMap: MutableMap<ProcessRecordKt, ScheduledFuture<*>> =
        ConcurrentHashMap()

    fun compactProcess(
        processRecord: ProcessRecordKt,
        lastOomScoreAdj: Int,
        curOomScoreAdj: Int,
        oomAdjustLevel: Int
    ) {
        if (processRecord.appInfo.appGroupEnum != RunningInfo.AppGroupEnum.IDLE) {
            return
        }
        if (lastOomScoreAdj == curOomScoreAdj) {
            return
        }

        compactProcessMap.compute(processRecord) {_, lastScheduledFuture->
            lastScheduledFuture?.cancel(true)

            executor.schedule({
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
            }, /*cachedAppOptimizer.mFreezerDebounceTimeout*/ 10 * 1000, TimeUnit.MILLISECONDS)
        }
    }

    private fun compactProcessImpl(
        processRecord: ProcessRecordKt,
        lastOomScoreAdj: Int,
        curOomScoreAdj: Int,
        oomAdjustLevel: Int
    ) {
        // 检验合法性
        if (!ProcessRecordKt.isValid(runningInfo, processRecord)) {
            return
        }

        // 是否进行了压缩
        var doCompact = false
        var processCompactResultCode = ProcessCompactResultCode.doNothing
        var processCompactEnum = ProcessCompactEnum.NONE
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
            if ((lastProcessCompatEnum == ProcessCompactEnum.SOME && timeDifference < cachedAppOptimizer.mCompactThrottleSomeSome)
                || (lastProcessCompatEnum == ProcessCompactEnum.FULL && timeDifference < cachedAppOptimizer.mCompactThrottleSomeFull)
            ) {
                // do nothing
            } else {
                doCompact = true
                processCompactEnum = ProcessCompactEnum.SOME
                compactAction = CachedAppOptimizer.COMPACT_ACTION_FILE
            }
        } else if (ProcessList.CACHED_APP_MIN_ADJ in lastOomScoreAdj..curOomScoreAdj
            || processRecord.isAllowedCompact(
                time = currentTimeMillis
            )/*.also { compactBecauseProcAllow = it }*/
        ) {
            if ((lastProcessCompatEnum == ProcessCompactEnum.SOME && timeDifference < cachedAppOptimizer.mCompactThrottleFullSome)
                || (lastProcessCompatEnum == ProcessCompactEnum.FULL && timeDifference < cachedAppOptimizer.mCompactThrottleFullFull)
            ) {
                // do nothing
            } else {
                /*if (compactBecauseProcAllow) {
                    compactReason = "从未压缩或超时"
                }*/
                doCompact = true
                processCompactEnum = ProcessCompactEnum.FULL
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

            processCompactResultCode =
                compactProcess(pid = processRecord.pid, compactAction = compactAction)
            updateProcessLastProcessingResult(processRecordKt = processRecord) {
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
        return if (cachedAppOptimizer.compactProcessForce(pid, compactAction)) {
            ProcessCompactResultCode.success
        } else {
            ProcessCompactResultCode.problem
        }
    }
}

class ProcessCompactProcessingResult : ProcessingResult() {
    var processCompactEnum = ProcessCompactEnum.NONE
}

enum class ProcessCompactEnum {
    NONE,
    SOME,
    FULL,
}