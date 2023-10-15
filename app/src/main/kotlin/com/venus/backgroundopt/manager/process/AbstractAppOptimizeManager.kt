package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2023/10/14
 */
abstract class AbstractAppOptimizeManager {
    // 任务的执行结果
    val processLastProcessingResultMap = ConcurrentHashMap<ProcessRecordKt, ProcessingResult>()

    inline fun updateProcessLastProcessingResult(
        processRecordKt: ProcessRecordKt,
        block: (ProcessingResult) -> Unit
    ) {
        val processingResult =
            processLastProcessingResultMap[processRecordKt] ?: run { ProcessingResult() }
        processingResult.lastProcessingTime = System.currentTimeMillis()

        block(processingResult)

        processLastProcessingResultMap[processRecordKt] = processingResult
    }

    fun removeProcessLastProcessingResult(processRecordKt: ProcessRecordKt) {
        processLastProcessingResultMap.remove(processRecordKt)
    }

    fun removeProcessLastProcessingResultFromSet(set:Set<ProcessRecordKt>) {
        set.forEach { process ->
            removeProcessLastProcessingResult(process)
        }
    }
}