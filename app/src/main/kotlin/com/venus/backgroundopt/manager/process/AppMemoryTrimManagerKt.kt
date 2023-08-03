package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.log.ILogger
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 应用内存清理管理器
 *
 * @author XingC
 * @date 2023/8/3
 */
class AppMemoryTrimManagerKt : ILogger {
    companion object {
        // 前台
        const val foregroundInitialDelay = 1L;
        const val foregroundDelay = 10L;
        val foregroundTimeUnit = TimeUnit.MINUTES
        const val foregroundTrimLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        const val foregroundTrimManagerName = "ForegroundAppMemoryTrimManager"

        // 后台
        const val backgroundInitialDelay = 1L;
        const val backgroundDelay = 10L;
        val backgroundTimeUnit = TimeUnit.MINUTES
        const val backgroundTrimLevel = ComponentCallbacks2.TRIM_MEMORY_MODERATE
        const val backgroundTrimManagerName = "BackgroundAppMemoryTrimManager"
    }

    // 线程池
    private val executor = ScheduledThreadPoolExecutor(2)

    private val foregroundTasks = ArrayList<ProcessRecord>()
    private val backgroundTasks = ArrayList<ProcessRecord>()

    init {
        init()
    }

    /**
     * 初始化
     */
    private fun init() {
        // 前台任务
        executor.scheduleWithFixedDelay({
            foregroundTasks.forEach {
                executeForegroundTask(it)
            }
        }, foregroundInitialDelay, foregroundDelay, foregroundTimeUnit)

        // 后台任务
        executor.scheduleWithFixedDelay({
            backgroundTasks.forEach {
                executeBackgroundTask(it)
            }
        }, backgroundInitialDelay, backgroundDelay, backgroundTimeUnit)
    }

    /**
     * 添加前台任务
     *
     * @param processRecord 进程记录器
     */
    fun addForegroundTask(processRecord: ProcessRecord?) {
        processRecord ?: run {
            if (BuildConfig.DEBUG) {
                logger.warn("processRecord为空设置个屁")
            }

            return
        }

        // 移除后台任务
        removeBackgroundTask(processRecord)

        foregroundTasks.add(processRecord)
    }

    /**
     * 添加后台任务
     *
     * @param processRecord 进程记录器
     */
    fun addBackgroundTask(processRecord: ProcessRecord?) {
        processRecord ?: run {
            if (BuildConfig.DEBUG) {
                logger.warn("processRecord为空移除个屁")
            }

            return
        }

        // 移除前台任务
        removeForegroundTask(processRecord)

        backgroundTasks.add(processRecord)
    }

    /**
     * 移除所有任务
     *
     * @param processRecord 进程记录器
     */
    fun removeAllTask(processRecord: ProcessRecord) {
        foregroundTasks.remove(processRecord)
        backgroundTasks.remove(processRecord)
    }

    /* *************************************************************************
    *                                                                         *
    * 前后台任务总调度                                                           *
    *                                                                         *
    **************************************************************************/
    /**
     * 执行前台任务
     *
     * @param processRecord 进程记录器
     */
    private fun executeForegroundTask(processRecord: ProcessRecord) {
        trimMemory(foregroundTrimManagerName, processRecord, foregroundTrimLevel)
    }

    private fun removeForegroundTask(processRecord: ProcessRecord) {
        removeTaskImpl(processRecord, foregroundTrimManagerName, foregroundTasks)
    }

    /**
     * 执行后台任务
     *
     * @param processRecord 进程记录器
     */
    private fun executeBackgroundTask(processRecord: ProcessRecord) {
        trimMemory(backgroundTrimManagerName, processRecord, backgroundTrimLevel)
        gc(processRecord)
    }

    private fun removeBackgroundTask(processRecord: ProcessRecord) {
        removeTaskImpl(processRecord, backgroundTrimManagerName, backgroundTasks)
    }

    private fun removeTaskImpl(
        processRecord: ProcessRecord,
        trimManagerName: String,
        list: ArrayList<ProcessRecord>
    ) {
        val remove = list.remove(processRecord)

        if (BuildConfig.DEBUG) {
            if (remove) {
                logger.debug(
                    "${trimManagerName}: ${processRecord.packageName}, uid: ${processRecord.uid} ->>> 移除Task"
                )
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 任务的具体实现                                                             *
     *                                                                         *
     **************************************************************************/
    private fun trimMemory(trimManagerName: String, processRecord: ProcessRecord, trimLevel: Int) {
        val result = processRecord.scheduleTrimMemory(trimLevel)

        if (result) {
            if (BuildConfig.DEBUG) {
                logger.debug(
                    "${trimManagerName}: ${processRecord.packageName}, uid: ${processRecord.uid} ->>> 设置TrimMemoryTask(${trimLevel}) 成功"
                )
            }
        } else {    // 若调用scheduleTrimMemory()后目标进程被终结(kill), 则会得到此结果
            logger.warn(
                "${trimManagerName}: ${processRecord.packageName}, uid: ${processRecord.uid} ->>> 设置TrimMemoryTask(${trimLevel}) 失败或未执行"
            )
        }
    }

    private fun gc(processRecord: ProcessRecord) {
        ProcessManager.handleGC(processRecord)
    }

    private class Tasks {}
}