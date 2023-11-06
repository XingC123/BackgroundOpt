package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt

/**
 * @author XingC
 * @date 2023/10/14
 */
abstract class AbstractAppOptimizeManager(val appOptimizeEnum: AppOptimizeEnum) {
    inline fun updateProcessLastProcessingResult(
        processRecordKt: ProcessRecordKt,
        block: (ProcessingResult) -> Unit
    ) {
        val processingResult =
            processRecordKt.lastProcessingResultMap.computeIfAbsent(appOptimizeEnum) { _ ->
                ProcessingResult()
            }

        processingResult.lastProcessingTime = System.currentTimeMillis()
        block(processingResult)
    }

    fun removeProcessLastProcessingResult(processRecordKt: ProcessRecordKt) {
        processRecordKt.lastProcessingResultMap.remove(appOptimizeEnum)
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
    }
}