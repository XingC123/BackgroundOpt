package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
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
class AppMemoryTrimManagerKt(private val activityManagerService: ActivityManagerService) : ILogger {
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

    val foregroundTasks: MutableSet<ProcessRecordKt> =
        Collections.newSetFromMap(ConcurrentHashMap())
    val backgroundTasks: MutableSet<ProcessRecordKt> =
        Collections.newSetFromMap(ConcurrentHashMap())

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
     * @param processRecordKt 进程记录器
     */
    fun addForegroundTask(processRecordKt: ProcessRecordKt?) {
        processRecordKt ?: run {
            if (BuildConfig.DEBUG) {
                logger.warn("processRecord为空设置个屁")
            }

            return
        }

        // 移除后台任务
        removeBackgroundTask(processRecordKt)

        val add = foregroundTasks.add(processRecordKt)
        if (BuildConfig.DEBUG) {
            if (add) {
                logger.debug(
                    "${
                        logStrPrefix(
                            foregroundTrimManagerName,
                            processRecordKt
                        )
                    }添加Task成功"
                )
            } else {
                logger.warn(
                    "${
                        logStrPrefix(
                            foregroundTrimManagerName,
                            processRecordKt
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
     * @param processRecordKt 进程记录器
     */
    fun addBackgroundTask(processRecordKt: ProcessRecordKt?) {
        processRecordKt ?: run {
            if (BuildConfig.DEBUG) {
                logger.warn("processRecord为空移除个屁")
            }

            return
        }

        // 移除前台任务
        removeForegroundTask(processRecordKt)

        val add = backgroundTasks.add(processRecordKt)

        if (BuildConfig.DEBUG) {
            if (add) {
                logger.debug(
                    "${
                        logStrPrefix(
                            backgroundTrimManagerName,
                            processRecordKt
                        )
                    }添加Task成功"
                )
            } else {
                logger.warn(
                    "${
                        logStrPrefix(
                            backgroundTrimManagerName,
                            processRecordKt
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
     * @param processRecordKt 进程记录器
     */
    fun removeAllTask(processRecordKt: ProcessRecordKt?) {
        processRecordKt?.let {
            foregroundTasks.remove(processRecordKt)
            backgroundTasks.remove(processRecordKt)

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
     * @param processRecordKt 进程记录器
     */
    private fun executeForegroundTask(processRecordKt: ProcessRecordKt) {
        trimMemory(foregroundTrimManagerName, processRecordKt, foregroundTrimLevel, foregroundTasks)
    }

    private fun removeForegroundTask(processRecordKt: ProcessRecordKt) {
        removeTaskImpl(processRecordKt, foregroundTrimManagerName, foregroundTasks)
    }

    /**
     * 执行后台任务
     *
     * @param processRecordKt 进程记录器
     */
    private fun executeBackgroundTask(processRecordKt: ProcessRecordKt) {
        trimMemory(backgroundTrimManagerName, processRecordKt, backgroundTrimLevel, backgroundTasks)
        gc(processRecordKt)
    }

    private fun removeBackgroundTask(processRecordKt: ProcessRecordKt) {
        removeTaskImpl(processRecordKt, backgroundTrimManagerName, backgroundTasks)
    }

    private fun removeTaskImpl(
        processRecordKt: ProcessRecordKt,
        trimManagerName: String,
        list: MutableSet<ProcessRecordKt>
    ) {
        val remove = list.remove(processRecordKt)

        if (BuildConfig.DEBUG) {
            if (remove) {
                logger.debug(
                    "${
                        logStrPrefix(
                            trimManagerName,
                            processRecordKt
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
        processRecordKt: ProcessRecordKt,
        trimLevel: Int,
        list: MutableSet<ProcessRecordKt>
    ) {
        processRecordKt.removeIfRedundant(list)
        val result = processRecordKt.scheduleTrimMemory(trimLevel)

        if (result) {
            if (BuildConfig.DEBUG) {
                logger.debug(
                    "${
                        logStrPrefix(
                            trimManagerName,
                            processRecordKt
                        )
                    }执行TrimMemoryTask(${trimLevel}) 成功"
                )
            }
        } else {    // 若调用scheduleTrimMemory()后目标进程被终结(kill), 则会得到此结果
            // 移除此任务
            list.remove(processRecordKt)

            logger.warn(
                "${
                    logStrPrefix(
                        trimManagerName,
                        processRecordKt
                    )
                }执行TrimMemoryTask(${trimLevel}) 失败或未执行"
            )
        }
    }

    private fun gc(processRecordKt: ProcessRecordKt) {
        if (!ProcessManager.handleGC(processRecordKt)) {
            backgroundTasks.remove(processRecordKt)
        }
    }

    private fun logStrPrefix(trimManagerName: String, processRecordKt: ProcessRecordKt): String {
        return "${trimManagerName}: ${processRecordKt.packageName}, uid: ${processRecordKt.uid} ->>> "
    }

    private class Tasks {}
}