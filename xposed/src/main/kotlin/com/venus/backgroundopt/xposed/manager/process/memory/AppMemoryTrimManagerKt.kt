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

package com.venus.backgroundopt.xposed.manager.process.memory

import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy
import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.android.content.ComponentCallbacks2
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.process.AppOptimizeEnum.PROCESS_MEM_TRIM
import com.venus.backgroundopt.xposed.manager.process.ProcessManager
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
    private val runningInfo: RunningInfo,
) : AbstractAppOptimizeManager(PROCESS_MEM_TRIM), ILogger {
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

    var enableForegroundTrim = HookCommonProperties.isEnableForegroundProcTrimMem()
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
    val foregroundTasks: MutableSet<ProcessRecord> =
        Collections.newSetFromMap(ConcurrentHashMap(4))
    val backgroundTasks: MutableSet<ProcessRecord> =
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
        enableForegroundTrim = HookCommonProperties.isEnableForegroundProcTrimMem()
        if (!enableForegroundTrim && foregroundTaskScheduledFuture == null) {
            logger.info("禁用: 前台进程内存回收")
        }

        // 后台任务
        if (HookCommonProperties.isEnableBackgroundProcTrimMem()) {
            executor.scheduleWithFixedDelay({
                backgroundTasks.forEach {
                    executeBackgroundTask(it)
                }
            }, backgroundInitialDelay, backgroundDelay, backgroundTimeUnit)
        } else {
            logger.info("禁用: 后台进程内存回收")
        }
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
                logger.info("启用: 前台进程内存回收。回收等级为: ${HookCommonProperties.getForegroundProcTrimMemLevelUiName()}")
            }
        }
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
        removeBackgroundFirstTrimTask(processRecord)
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

        addBackgroundFirstTrimTask(processRecord)

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

    override fun getBackgroundFirstTaskDelay(): Long = backgroundFirstTrimDelay

    override fun getBackgroundFirstTaskDelayTimeUnit(): TimeUnit = backgroundFirstTrimTimeUnit

    private fun addBackgroundFirstTrimTask(processRecord: ProcessRecord) {
        addBackgroundFirstTask(processRecord = processRecord) {
            runCatchThrowable {
                // UI资源的清理
                processRecord.scheduleTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

                if (BuildConfig.DEBUG) {
                    logger.info("packageName: ${processRecord.packageName}, processName: ${processRecord.processName}, userId: ${processRecord.userId}, 执行TrimMemoryTask(${ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN})成功")
                }
            }
        }
    }

    private fun removeBackgroundFirstTrimTask(processRecord: ProcessRecord) {
        removeBackgroundFirstTask(processRecord = processRecord)
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
            backgroundFirstTaskMap.remove(processRecord)
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
        list: MutableSet<ProcessRecord>,
        processRecord: ProcessRecord,
        block: (AppOptimizePolicy?) -> Unit,
    ) {
        // 获取优化操作
        val appOptimizePolicy = HookCommonProperties.appOptimizePolicyMap[processRecord.packageName]

        if (isNecessaryToOptimizeProcess(processRecord)) {
            block(appOptimizePolicy)
        } else {
            if (BuildConfig.DEBUG) {
                logger.debug("uid: ${processRecord.uid}, pid: ${processRecord.pid}, 包名: ${processRecord.packageName}不需要优化")
            }
        }
    }

    /**
     * 执行前台任务
     *
     * @param processRecord 进程记录器
     */
    private fun executeForegroundTask(processRecord: ProcessRecord) {
        executeTaskImpl(
            foregroundTrimManagerName,
            foregroundTasks,
            processRecord
        ) { appOptimizePolicy ->
            appOptimizePolicy?.let { policy ->
                if (policy.enableForegroundTrimMem == false) {
                    return@executeTaskImpl
                }
            }
            trimMemory(
                foregroundTrimManagerName,
                processRecord,
                HookCommonProperties.getForegroundProcTrimMemLevel(),
                foregroundTasks
            )
        }
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
        executeTaskImpl(
            backgroundTrimManagerName,
            backgroundTasks,
            processRecord
        ) { appOptimizePolicy ->
            appOptimizePolicy?.let { policy ->
                if (policy.enableBackgroundTrimMem != false) {
                    trimMemory(
                        backgroundTrimManagerName,
                        processRecord,
                        backgroundTrimLevel,
                        backgroundTasks
                    )
                }
                if (policy.enableBackgroundGc != false) {
                    gc(processRecord)
                }
                return@executeTaskImpl
            }
            /*
                默认执行的操作
             */
            trimMemory(
                backgroundTrimManagerName,
                processRecord,
                backgroundTrimLevel,
                backgroundTasks
            )
            // debug_632 版本开始默认不执行
            // gc(processRecordKt)
        }
    }

    private fun removeBackgroundTask(processRecord: ProcessRecord) {
        removeTaskImpl(processRecord, backgroundTrimManagerName, backgroundTasks)
    }

    private fun removeTaskImpl(
        processRecord: ProcessRecord,
        trimManagerName: String,
        list: MutableSet<ProcessRecord>,
        removeReason: String = "",
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
        processRecord: ProcessRecord,
        trimLevel: Int,
        list: MutableSet<ProcessRecord>,
    ) {
        val result = run {
            if (backgroundFirstTaskMap.contains(processRecord)) {
                null
            } else {
                processRecord.scheduleTrimMemory(trimLevel)
            }
        }

        if (result == null) {
            logger.info(
                "${
                    logStrPrefix(
                        trimManagerName,
                        processRecord
                    )
                }执行TrimMemoryTask(${trimLevel}): 已有任务正在等待执行(初次进入后台的回收)"
            )
        } else if (result) {
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
        if (!ProcessManager.handleGC(processRecord)) {
            backgroundTasks.remove(processRecord)
        }
    }

    private fun logStrPrefix(trimManagerName: String, processRecord: ProcessRecord): String {
        return "${trimManagerName}: ${processRecord.packageName}, uid: ${processRecord.uid} ->>> "
    }

    private class Tasks {}
}