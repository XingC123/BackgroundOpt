package com.venus.backgroundopt.manager.process

import android.os.SystemClock
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
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
    private val executor = ScheduledThreadPoolExecutor(1).apply {
        removeOnCancelPolicy = true
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
        curOomScoreAdj: Int
    ) {
        if (processRecord.appInfo.appGroupEnum != RunningInfo.AppGroupEnum.IDLE) {
            return
        }
        if (lastOomScoreAdj == curOomScoreAdj) {
            return
        }

        compactProcessMap.remove(processRecord)?.cancel(true)
        compactProcessMap[processRecord] = executor.schedule({
            runCatchThrowable(catchBlock = {
                logger.error("压缩进程任务出错", it)
            }) {
                compactProcessImpl(
                    processRecord = processRecord,
                    lastOomScoreAdj = lastOomScoreAdj,
                    curOomScoreAdj = curOomScoreAdj
                )
            }
        }, 0, TimeUnit.SECONDS)
    }

    private fun compactProcessImpl(
        processRecord: ProcessRecordKt,
        lastOomScoreAdj: Int,
        curOomScoreAdj: Int
    ) {
        // 检验合法性
        if (!ProcessRecordKt.isValid(runningInfo, processRecord)) {
            return
        }

        // 检查进程是否需要进行优化
        if (!processRecord.isNecessaryToOptimize()) {
            updateProcessLastProcessingResult(processRecord) {
                it.lastProcessingCode = ProcessCompactResultCode.unNecessary
            }

            if (BuildConfig.DEBUG) {
                logger.debug("uid: ${processRecord.uid}, pid: ${processRecord.pid}, 主进程: ${processRecord.mainProcess}, 包名: ${processRecord.packageName}不需要压缩")
            }
            return
        }

        // 是否进行了压缩
        var doCompact = false
        var processCompactResultCode = ProcessCompactResultCode.doNothing
        var processCompatEnum = ProcessCompatEnum.NONE
        val processingResult = processRecord.initLastProcessingResultIfAbsent(
                appOptimizeEnum = appOptimizeEnum,
                processingResultSupplier = ::getProcessingResultIfAbsent
            ) as ProcessCompactProcessingResult
        val currentTimeMillis = SystemClock.uptimeMillis()
        val timeDifference = currentTimeMillis - processingResult.lastProcessingTime
        val lastProcessCompatEnum = processingResult.processCompatEnum
        var compactAction = Int.MIN_VALUE
        var compactReason = "OOM_SCORE"
        var compactBecauseProcAllow = false

        if (ProcessList.PERCEPTIBLE_APP_ADJ in lastOomScoreAdj..curOomScoreAdj && curOomScoreAdj <= ProcessList.SERVICE_B_ADJ) {
            if ((lastProcessCompatEnum == ProcessCompatEnum.SOME && timeDifference < cachedAppOptimizer.mCompactThrottleSomeSome)
                || (lastProcessCompatEnum == ProcessCompatEnum.FULL && timeDifference < cachedAppOptimizer.mCompactThrottleSomeFull)
            ) {
                // do nothing
            } else {
                doCompact = true
                processCompatEnum = ProcessCompatEnum.SOME
                compactAction = CachedAppOptimizer.COMPACT_ACTION_FILE
            }
        } else if (ProcessList.CACHED_APP_MIN_ADJ in lastOomScoreAdj..curOomScoreAdj
            || processRecord.isAllowedCompact(
                time = currentTimeMillis
            ).also { compactBecauseProcAllow = it }
        ) {
            if ((lastProcessCompatEnum == ProcessCompatEnum.SOME && timeDifference < cachedAppOptimizer.mCompactThrottleFullSome)
                || (lastProcessCompatEnum == ProcessCompatEnum.FULL && timeDifference < cachedAppOptimizer.mCompactThrottleFullFull)
            ) {
                // do nothing
            } else {
                if (compactBecauseProcAllow) {
                    compactReason = "从未压缩或超时"
                }
                doCompact = true
                processCompatEnum = ProcessCompatEnum.FULL
                compactAction = CachedAppOptimizer.COMPACT_ACTION_FULL
            }
        }

        if (doCompact) {
            processCompactResultCode =
                compactProcess(pid = processRecord.pid, compactAction = compactAction)
            updateProcessLastProcessingResult(processRecordKt = processRecord) {
                processingResult.lastProcessingCode = processCompactResultCode
                processingResult.processCompatEnum = processCompatEnum
            }
        }

        if (BuildConfig.DEBUG) {
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
        }
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
}

class ProcessCompactProcessingResult : ProcessingResult() {
    var processCompatEnum = ProcessCompatEnum.NONE
}

enum class ProcessCompatEnum {
    NONE,
    SOME,
    FULL,
}