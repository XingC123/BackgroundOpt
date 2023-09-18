package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.log.ILogger
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
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
        const val foregroundInitialDelay = 0L
        const val foregroundDelay = 10L
        val foregroundTimeUnit = TimeUnit.MINUTES
        const val foregroundTrimLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        const val foregroundTrimManagerName = "ForegroundAppMemoryTrimManager"

        // 后台
        const val backgroundInitialDelay = 2L  // 初始时间相比前台任务延后2个单位时间
        const val backgroundDelay = 10L
        val backgroundTimeUnit = TimeUnit.MINUTES
        const val backgroundTrimLevel = ComponentCallbacks2.TRIM_MEMORY_MODERATE
        const val backgroundTrimManagerName = "BackgroundAppMemoryTrimManager"
    }

    // 线程池
    // 23.9.14: 仅分配一个线程, 防止前后台任务同时进行造成可能的掉帧
    private val executor = ScheduledThreadPoolExecutor(1)

    private val foregroundTasks = Collections.newSetFromMap<ProcessRecord>(ConcurrentHashMap())
    private val backgroundTasks = Collections.newSetFromMap<ProcessRecord>(ConcurrentHashMap())

    init {
        init()
    }

    /**
     * 初始化
     */
    private fun init() {
        // 前台任务
        /*
            23.9.18: 前台任务并没有严格的单独提交ScheduledFuture, 而是加入到统一检查组,
                这意味着某些情况下, 个别前台甚至不会被执行(前台那么积极干嘛)
         */
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
                    }添加Task失败或Task已存在"
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
        processRecord?.let {
            foregroundTasks.remove(processRecord)
            backgroundTasks.remove(processRecord)

            if (BuildConfig.DEBUG) {
                logger.debug("foregroundTasks元素个数: ${foregroundTasks.size}, backgroundTasks元素个数: ${backgroundTasks.size}")
            }
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