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

    private val foregroundTasks = mutableSetOf<ProcessRecord>()
    private val backgroundTasks = mutableSetOf<ProcessRecord>()

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
        }, 0, foregroundDelay, foregroundTimeUnit)

        // 后台任务
        executor.scheduleWithFixedDelay({
            backgroundTasks.forEach {
                executeBackgroundTask(it)
            }
        }, 0, backgroundDelay, backgroundTimeUnit)
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

        val add = foregroundTasks.add(processRecord)
        if (BuildConfig.DEBUG) {
            if (add) {
                logger.debug(
                    "${
                        logStrPrefix(
                            foregroundTrimManagerName,
                            processRecord
                        )
                    }添加Task成功"
                )
            } else {
                logger.warn(
                    "${
                        logStrPrefix(
                            foregroundTrimManagerName,
                            processRecord
                        )
                    }添加Task失败"
                )
            }
        }

        if (BuildConfig.DEBUG) {
            logger.debug("foregroundTasks元素个数: ${foregroundTasks.size}, backgroundTasks元素个数: ${backgroundTasks.size}")
        }
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

        val add = backgroundTasks.add(processRecord)

        if (BuildConfig.DEBUG) {
            if (add) {
                logger.debug(
                    "${
                        logStrPrefix(
                            backgroundTrimManagerName,
                            processRecord
                        )
                    }添加Task成功"
                )
            } else {
                logger.warn(
                    "${
                        logStrPrefix(
                            backgroundTrimManagerName,
                            processRecord
                        )
                    }添加Task失败"
                )
            }
        }

        if (BuildConfig.DEBUG) {
            logger.debug("foregroundTasks元素个数: ${foregroundTasks.size}, backgroundTasks元素个数: ${backgroundTasks.size}")
        }
    }

    /**
     * 移除所有任务
     *
     * @param processRecord 进程记录器
     */
    fun removeAllTask(processRecord: ProcessRecord?) {
        foregroundTasks.remove(processRecord)
        backgroundTasks.remove(processRecord)

        if (BuildConfig.DEBUG) {
            logger.debug("foregroundTasks元素个数: ${foregroundTasks.size}, backgroundTasks元素个数: ${backgroundTasks.size}")
        }
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
        trimMemory(foregroundTrimManagerName, processRecord, foregroundTrimLevel, foregroundTasks)
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
        trimMemory(backgroundTrimManagerName, processRecord, backgroundTrimLevel, backgroundTasks)
        gc(processRecord)
    }

    private fun removeBackgroundTask(processRecord: ProcessRecord) {
        removeTaskImpl(processRecord, backgroundTrimManagerName, backgroundTasks)
    }

    private fun removeTaskImpl(
        processRecord: ProcessRecord,
        trimManagerName: String,
        list: MutableSet<ProcessRecord>
    ) {
        val remove = list.remove(processRecord)

        if (BuildConfig.DEBUG) {
            if (remove) {
                logger.debug(
                    "${
                        logStrPrefix(
                            trimManagerName,
                            processRecord
                        )
                    }移除Task"
                )
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 任务的具体实现                                                             *
     *                                                                         *
     **************************************************************************/
    private fun trimMemory(
        trimManagerName: String,
        processRecord: ProcessRecord,
        trimLevel: Int,
        list: MutableSet<ProcessRecord>
    ) {
        val result = processRecord.scheduleTrimMemory(trimLevel)

        if (result) {
            if (BuildConfig.DEBUG) {
                logger.debug(
                    "${
                        logStrPrefix(
                            trimManagerName,
                            processRecord
                        )
                    }执行TrimMemoryTask(${trimLevel}) 成功"
                )
            }
        } else {    // 若调用scheduleTrimMemory()后目标进程被终结(kill), 则会得到此结果
            // 移除此任务
            list.remove(processRecord)

            logger.warn(
                "${
                    logStrPrefix(
                        trimManagerName,
                        processRecord
                    )
                }执行TrimMemoryTask(${trimLevel}) 失败或未执行"
            )
        }
    }

    private fun gc(processRecord: ProcessRecord) {
        ProcessManager.handleGC(processRecord)
    }

    private fun logStrPrefix(trimManagerName: String, processRecord: ProcessRecord): String {
        return "${trimManagerName}: ${processRecord.packageName}, uid: ${processRecord.uid} ->>> "
    }

    private class Tasks {}
}