package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.runCatchThrowable
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 应用内存清理管理器
 *
 * @author XingC
 * @date 2023/8/3
 */
class AppMemoryTrimManagerKt(
    private val runningInfo: RunningInfo
) : AbstractAppOptimizeManager(AppOptimizeEnum.PROCESS_MEM_TRIM), ILogger {
    companion object {
        // 前台
        const val foregroundInitialDelay = 1L
        const val foregroundDelay = 10L
        val foregroundTimeUnit = TimeUnit.MINUTES
        const val foregroundTrimManagerName = "ForegroundAppMemoryTrimManager"

        // 后台
        const val backgroundInitialDelay = 2L  // 初始时间相比前台任务延后2个单位时间
        const val backgroundDelay = 10L
        val backgroundTimeUnit = TimeUnit.MINUTES
        const val backgroundTrimLevel = PreferenceDefaultValue.backgroundProcMemTrimLevel
        const val backgroundTrimManagerName = "BackgroundAppMemoryTrimManager"

        // 初次进入后台
        const val backgroundFirstTrimDelay = 30L
        val backgroundFirstTrimTimeUnit = TimeUnit.SECONDS
    }

    var enableForegroundTrim = CommonProperties.isEnableForegroundProcTrimMem()
        set(value) {
            field = value

            configureForegroundTrimCheckTask(value)
        }

    @Volatile
    private var isForegroundTaskRunning = false

    @Volatile
    var foregroundTaskScheduledFuture: ScheduledFuture<*>? = null

    // 线程池
    // 23.9.14: 仅分配一个线程, 防止前后台任务同时进行造成可能的掉帧
    private val executor = ScheduledThreadPoolExecutor(1).apply {
        removeOnCancelPolicy = true
    }

    /**
     * 初始容量为4。此列表放置的是app主进程, 因此同步于[RunningInfo.activeAppGroup]的初始容量
     */
    val foregroundTasks: MutableSet<ProcessRecordKt> =
        Collections.newSetFromMap(ConcurrentHashMap(4))
    val backgroundTasks: MutableSet<ProcessRecordKt> =
        Collections.newSetFromMap(ConcurrentHashMap())

    init {
        init()
    }

    override fun getExecutor(): Executor = executor

    /**
     * 初始化
     */
    private fun init() {
        // 前台任务
        enableForegroundTrim = CommonProperties.isEnableForegroundProcTrimMem()
        if (!enableForegroundTrim && foregroundTaskScheduledFuture == null) {
            logger.info("禁用: 前台进程内存回收")
        }

        // 后台任务
        executor.scheduleWithFixedDelay({
            backgroundTasks.forEach {
                executeBackgroundTask(it)
            }
        }, backgroundInitialDelay, backgroundDelay, backgroundTimeUnit)
    }

    private fun configureForegroundTrimCheckTask(isEnable: Boolean) {
        foregroundTaskScheduledFuture?.let { scheduledFuture ->
            if (!isEnable) {
                if (!isForegroundTaskRunning) {
                    scheduledFuture.cancel(true)
                    foregroundTaskScheduledFuture = null
                    logger.info("禁用: 前台进程内存回收")
                } else {
                    logger.info("禁用: 前台进程内存回收(等待当前任务执行完毕)")
                }
            }
        } ?: run {
            if (isEnable) {
                /*
                     23.9.18: 前台任务并没有严格的单独提交ScheduledFuture, 而是加入到统一检查组,
                        这意味着某些情况下, 个别前台甚至不会被执行(前台那么积极干嘛)
                 */
                foregroundTaskScheduledFuture = executor.scheduleWithFixedDelay({
                    isForegroundTaskRunning = true
                    foregroundTasks.forEach {
                        executeForegroundTask(it)
                    }
                    isForegroundTaskRunning = false
                    if (!enableForegroundTrim) {
                        foregroundTaskScheduledFuture?.let {
                            it.cancel(true)
                            foregroundTaskScheduledFuture = null
                            logger.info("禁用: 前台进程内存回收")
                        }
                    }
                }, foregroundInitialDelay, foregroundDelay, foregroundTimeUnit)
                logger.info("启用: 前台进程内存回收。回收等级为: ${CommonProperties.getForegroundProcTrimMemLevelUiName()}")
            }
        }
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

        ProcessRecordKt.correctProcessPid(processRecordKt)

        // 移除后台任务
        removeBackgroundFirstTrimTask(processRecordKt)
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

        ProcessRecordKt.correctProcessPid(processRecordKt)

        // 移除前台任务
        removeForegroundTask(processRecordKt)

        addBackgroundFirstTrimTask(processRecordKt)

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

    override fun getBackgroundFirstTaskDelay(): Long = backgroundFirstTrimDelay

    override fun getBackgroundFirstTaskDelayTimeUnit(): TimeUnit = backgroundFirstTrimTimeUnit

    private fun addBackgroundFirstTrimTask(processRecordKt: ProcessRecordKt) {
        addBackgroundFirstTask(processRecordKt = processRecordKt) {
            runCatchThrowable {
                // UI资源的清理
                processRecordKt.scheduleTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            }
        }
    }

    private fun removeBackgroundFirstTrimTask(processRecordKt: ProcessRecordKt) {
        removeBackgroundFirstTask(processRecordKt = processRecordKt)
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
    private fun executeTaskImpl(
        trimManagerName: String,
        list: MutableSet<ProcessRecordKt>,
        processRecordKt: ProcessRecordKt,
        block: (AppOptimizePolicy?) -> Unit
    ) {
        if (!ProcessRecordKt.isValid(runningInfo, processRecordKt)) {
            removeTaskImpl(processRecordKt, trimManagerName, list, "进程不合法")
            return
        }

        // 获取优化操作
        val appOptimizePolicy = CommonProperties.appOptimizePolicyMap[processRecordKt.packageName]

        if (processRecordKt.isNecessaryToOptimize()) {
            block(appOptimizePolicy)
        } else {
            if (BuildConfig.DEBUG) {
                logger.debug("uid: ${processRecordKt.uid}, pid: ${processRecordKt.pid}, 包名: ${processRecordKt.packageName}不需要优化")
            }
        }
    }

    /**
     * 执行前台任务
     *
     * @param processRecordKt 进程记录器
     */
    private fun executeForegroundTask(processRecordKt: ProcessRecordKt) {
        executeTaskImpl(
            foregroundTrimManagerName,
            foregroundTasks,
            processRecordKt
        ) { appOptimizePolicy ->
            appOptimizePolicy?.let { policy ->
                if (policy.enableForegroundTrimMem == false) {
                    return@executeTaskImpl
                }
            }
            trimMemory(
                foregroundTrimManagerName,
                processRecordKt,
                CommonProperties.getForegroundProcTrimMemLevel(),
                foregroundTasks
            )
        }
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
        executeTaskImpl(
            backgroundTrimManagerName,
            backgroundTasks,
            processRecordKt
        ) { appOptimizePolicy ->
            appOptimizePolicy?.let { policy ->
                if (policy.enableBackgroundTrimMem != false) {
                    trimMemory(
                        backgroundTrimManagerName,
                        processRecordKt,
                        backgroundTrimLevel,
                        backgroundTasks
                    )
                }
                if (policy.enableBackgroundGc != false) {
                    gc(processRecordKt)
                }
                return@executeTaskImpl
            }
            /*
                默认执行的操作
             */
            trimMemory(
                backgroundTrimManagerName,
                processRecordKt,
                backgroundTrimLevel,
                backgroundTasks
            )
            // debug_632 版本开始默认不执行
            // gc(processRecordKt)
        }
    }

    private fun removeBackgroundTask(processRecordKt: ProcessRecordKt) {
        removeTaskImpl(processRecordKt, backgroundTrimManagerName, backgroundTasks)
    }

    private fun removeTaskImpl(
        processRecordKt: ProcessRecordKt,
        trimManagerName: String,
        list: MutableSet<ProcessRecordKt>,
        removeReason: String = ""
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
                    }移除Task" + if (removeReason.isEmpty()) removeReason else ": reason: $removeReason"
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
        val result = run {
            if (backgroundFirstTaskMap.contains(processRecordKt)) {
                null
            } else {
                processRecordKt.scheduleTrimMemory(trimLevel)
            }
        }

        if (result == null) {
            logger.info(
                "${
                    logStrPrefix(
                        trimManagerName,
                        processRecordKt
                    )
                }执行TrimMemoryTask(${trimLevel}): 已有任务正在等待执行(初次进入后台的回收)"
            )
        } else if (result) {
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