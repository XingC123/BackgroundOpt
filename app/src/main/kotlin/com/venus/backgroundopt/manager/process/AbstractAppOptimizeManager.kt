package com.venus.backgroundopt.manager.process

import android.os.SystemClock
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
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

    open fun isNecessaryToOptimizeProcess(processRecordKt: ProcessRecordKt): Boolean {
        val globalOomScorePolicy = CommonProperties.globalOomScorePolicy.value
        val enabledGlobalOomScore = globalOomScorePolicy.enabled
        val isUnsafeCustomOomScore = globalOomScorePolicy.customGlobalOomScore < 0
        return if (enabledGlobalOomScore && isUnsafeCustomOomScore) {
            processRecordKt.isNecessaryToOptimize()
        } else {
            processRecordKt.oomAdjScore >= 0 && processRecordKt.isNecessaryToOptimize()
        }
    }

    open fun getBackgroundFirstTaskDelay(): Long = 0

    open fun getBackgroundFirstTaskDelayTimeUnit(): TimeUnit = TimeUnit.SECONDS

    val backgroundFirstTaskMap: MutableMap<ProcessRecordKt, ScheduledFuture<*>> =
        ConcurrentHashMap(4)

    open fun addBackgroundFirstTask(
        processRecordKt: ProcessRecordKt,
        block: (() -> Unit)? = null,
    ) {
        backgroundFirstTaskMap.computeIfAbsent(processRecordKt) { _ ->
            (getExecutor() as ScheduledThreadPoolExecutor).schedule(
                { block?.let { it() } },
                getBackgroundFirstTaskDelay(),
                getBackgroundFirstTaskDelayTimeUnit()
            )
        }
    }

    open fun removeBackgroundFirstTask(
        processRecordKt: ProcessRecordKt,
        block: ((ScheduledFuture<*>) -> Unit)? = null,
    ) {
        backgroundFirstTaskMap.remove(processRecordKt)?.let { scheduledFuture ->
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
        processRecordKt: ProcessRecordKt,
        appOptEnum: AppOptimizeEnum = appOptimizeEnum,
        block: (ProcessingResult) -> Unit
    ) {
        val processingResult = processRecordKt.initLastProcessingResultIfAbsent(
            appOptimizeEnum = appOptEnum,
            processingResultSupplier = ::getProcessingResultIfAbsent
        )

        processingResult.lastProcessingTime = SystemClock.uptimeMillis()
        block(processingResult)
    }

    fun removeProcessLastProcessingResult(
        processRecordKt: ProcessRecordKt,
        appOptEnum: AppOptimizeEnum = appOptimizeEnum,
    ) {
        processRecordKt.lastProcessingResultMap.remove(appOptEnum)
    }

    fun removeProcessLastProcessingResultFromSet(set: Set<ProcessRecordKt>) {
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