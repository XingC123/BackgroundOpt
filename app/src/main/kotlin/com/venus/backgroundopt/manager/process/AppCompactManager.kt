package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.annotation.UsageComment
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.concurrent.lock
import com.venus.backgroundopt.utils.log.ILogger
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 应用内存压缩管理器
 * @author XingC
 * @date 2023/8/8
 */
class AppCompactManager(// 封装的CachedAppOptimizer
    private val cachedAppOptimizer: CachedAppOptimizer,
    private val runningInfo: RunningInfo
) : AbstractAppOptimizeManager(), ILogger {
    companion object {
        // 默认压缩级别
        const val DEFAULT_COMPACT_LEVEL = CachedAppOptimizer.COMPACT_ACTION_ANON

        // App压缩扫描线程池配置
        const val initialDelay = 5L
        const val delay = 10L
        val timeUnit = TimeUnit.MINUTES
    }

    // 是否启用"压缩成功则移除进程" + "无内存压缩任务则关闭轮循"
    var autoStopCompactTask = CommonProperties.getAutoStopCompactTaskPreferenceValue()
        set(value) {
            field = value

            compactScheduledFuture?.let { future ->
                if (value && compactProcesses.size == 0) {
                    stopCompactTask(future)
                }
            } ?: run {
                if (!value) {
                    startCompactTask()
                }
            }
        }

    // App压缩处理线程池
    private val executor = ScheduledThreadPoolExecutor(1).apply {
        removeOnCancelPolicy = true
    }

    // 待压缩的进程信息列表
    // 注意: 是"进程信息"而不是"应用信息", 其size代表的是进程数不是app数
    val compactProcesses: MutableSet<ProcessRecordKt> =
        Collections.newSetFromMap(ConcurrentHashMap())

    init {
        // 如果此项未启用, 则直接启动轮循任务
        logger.info("压缩任务自动关闭: $autoStopCompactTask")
        if (!autoStopCompactTask) {
            startCompactTask()
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 压缩任务处理                                                               *
     *                                                                         *
     **************************************************************************/
    @Volatile
    private var compactScheduledFuture: ScheduledFuture<*>? = null
    private fun startCompactTask() {
        compactScheduledFuture = executor.scheduleWithFixedDelay({
            var compactCount = 0
            val upgradeSubProcessNames = CommonProperties.getUpgradeSubProcessNames()
            compactProcesses.forEach { process ->
                // 检验合法性
                if (!ProcessRecordKt.isValid(runningInfo, process)) {
                    cancelCompactProcess(process, "进程不合法")
                    return@forEach
                }

                if (!process.isNecessaryToOptimize()) {
                    updateProcessLastProcessingResult(process) {
                        it.lastProcessingCode = ProcessCompactResultCode.unNecessary
                    }

                    if (BuildConfig.DEBUG) {
                        logger.debug("uid: ${process.uid}, pid: ${process.pid}, 主进程: ${process.mainProcess}, 包名: ${process.packageName}不需要压缩")
                    }
                    return@forEach
                }

                // 根据默认规则压缩
                var compactMethod: (processRecordKt: ProcessRecordKt) -> Int = ::compactAppFull

                if (process.mainProcess || upgradeSubProcessNames.contains(process.processName)) {
                    val currentTime = System.currentTimeMillis()
                    if (!process.isAllowedCompact(currentTime)) {  // 若压缩间隔不满足, 则跳过等待下一轮
                        updateProcessLastProcessingResult(process) {
                            it.lastProcessingCode = ProcessCompactResultCode.doNothing
                        }

                        return@forEach
                    }
                    // 压缩条件由ProcessInfo.isAllowedCompact判断。因此, 符合条件后直接调用压缩
                    compactMethod = ::compactAppFullNoCheck

                    process.setLastCompactTime(currentTime)
                }

                when (val result = compactMethod(process)) {
                    ProcessCompactResultCode.success, ProcessCompactResultCode.doNothing -> {
                        if (result == ProcessCompactResultCode.success) {
                            if (autoStopCompactTask) {
                                cancelCompactProcess(process)
                            }
                            compactCount++

                            if (BuildConfig.DEBUG) {
                                logger.debug(
                                    "uid: ${process.uid}, pid: ${process.pid} >>> " +
                                            "因[OOM_SCORE] 而内存压缩"
                                )
                            }
                        }
                        updateProcessLastProcessingResult(process) {
                            it.lastProcessingCode = result
                        }
                    }

                    else -> {   // ProcessCompactResultCode.problem
                        cancelCompactProcess(process)

                        if (BuildConfig.DEBUG) {
                            logger.warn(
                                "uid: ${process.uid}, pid: ${process.pid} >>> " +
                                        "因[OOM_SCORE] 而内存压缩: 发生异常!压缩执行失败或进程已停止"
                            )
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                logger.debug("本次压缩了[${compactCount}]个进程, 列表共[${compactProcesses.size}]个进程")
            }
        }, initialDelay, delay, timeUnit)

        compactScheduledFuture?.let {
            if (BuildConfig.DEBUG) {
                logger.debug("创建内存压缩检查任务成功")
            }
        }
    }

    private fun checkCompactTask() {
        if (autoStopCompactTask) {
            compactScheduledFuture?.let {
                if (compactProcesses.size == 0) {    // 若此时没有待压缩任务, 则取消检查任务
                    stopCompactTask(it)
                }
            } ?: startCompactTask() // 需要待压缩任务, 创建
        }
    }

    private fun stopCompactTask(future: ScheduledFuture<*>) {
        future.cancel(true)
        compactScheduledFuture = null
        if (BuildConfig.DEBUG) {
            logger.debug("待压缩列表为空, 停止检查任务")
        }
    }

    /**
     * 添加压缩进程
     */
    fun addCompactProcess(appInfo: AppInfo) {
        val processRecordKts = appInfo.processes
        if (processRecordKts.isNotEmpty()) {
            val currentTimeMillis = System.currentTimeMillis()
            processRecordKts.forEach { addCompactProcessImpl(it, currentTimeMillis) }
            checkCompactTask()

            if (BuildConfig.DEBUG) {
                logger.debug("uid: ${processRecordKts.first().uid} >>> [批量]加入待压缩列表")
            }
        }
    }

    fun addCompactProcess(processRecordKt: ProcessRecordKt) {
        addCompactProcessImpl(processRecordKt)
        checkCompactTask()

        if (BuildConfig.DEBUG) {
            logger.debug("uid: ${processRecordKt.uid}, pid: ${processRecordKt.pid} >>> 加入待压缩列表")
        }
    }

    /**
     * 在确保已对[AppInfo]上锁的情况下再使用
     *
     * @param processRecordKt
     */
    @UsageComment("确保AppInfo已上锁")
    private fun addCompactProcessImpl(
        processRecordKt: ProcessRecordKt,
        lastCompactTime: Long = System.currentTimeMillis()
    ) {
        compactProcesses.add(processRecordKt.also {
            it.setLastCompactTime(lastCompactTime)
        })
    }

    /**
     * 移除压缩进程
     */
    @JvmOverloads
    fun cancelCompactProcess(processRecordKt: ProcessRecordKt?, cancelReason: String = "") {
        processRecordKt?.let { process ->
            compactProcesses.remove(process).also {
                if (it) {
                    removeProcessLastProcessingResult(processRecordKt)
                    checkCompactTask()

                    if (BuildConfig.DEBUG) {
                        logger.debug(
                            "uid: ${process.uid}, pid: ${process.pid} >>> 移除自待压缩列表" +
                                    if (cancelReason.isEmpty()) cancelReason else ": reason: $cancelReason"
                        )
                    }
                }
            }
        }
    }

    fun cancelCompactProcess(appInfo: AppInfo) {
        var set: Set<ProcessRecordKt>? = null
        appInfo.lock { set = compactProcesses.filter { it.uid == appInfo.uid }.toSet() }
        set?.let { setFromUid ->
            if (setFromUid.isNotEmpty()) {
                compactProcesses.removeAll(setFromUid).also {
                    if (it) {
                        removeProcessLastProcessingResultFromSet(setFromUid)
                        checkCompactTask()

                        if (BuildConfig.DEBUG) {
                            logger.debug("uid: ${setFromUid.first().uid} >>> 移除自待压缩列表")
                        }
                    }
                }
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 压缩方式                                                                  *
     *                                                                         *
     **************************************************************************/
    fun compactApp(processRecordKt: ProcessRecordKt) {
        compactApp(processRecordKt.pid)
    }

    fun compactApp(pid: Int) {
        compactApp(pid, DEFAULT_COMPACT_LEVEL)
    }

    /**
     * 压缩app
     *
     * @param pid           进程pid
     * @param compactAction 压缩行为: [CachedAppOptimizer.COMPACT_ACTION_NONE]等
     */
    fun compactApp(pid: Int, compactAction: Int): Boolean {
        return cachedAppOptimizer.compactProcess(pid, compactAction)
    }

    /**
     * 部分压缩
     *
     * @param pid 要压缩的pid
     */
    fun compactAppSome(pid: Int) {
        compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FILE)
        if (BuildConfig.DEBUG) {
            logger.debug("pid: $pid >>> 进行了一次compactAppSome")
        }
    }

    fun compactAppSome(appInfo: AppInfo) {
        appInfo.processPids.forEach { pid: Int ->
            this.compactAppSome(pid)
        }
    }

    /**
     * 全量压缩
     * 源码中(com.android.server.am.CachedAppOptimizer.DefaultProcessDependencies.performCompaction(String action, int pid)),
     * 传入到compactApp(pid: Int, compactAction: Int)中的compactAction为: [CachedAppOptimizer.COMPACT_ACTION_FILE] | [CachedAppOptimizer.COMPACT_ACTION_ANON],
     * 即最终结果为: [CachedAppOptimizer.COMPACT_ACTION_FULL]
     *
     * @param pid 要压缩的pid
     * @return 见[ProcessCompactResultCode]
     */
    fun compactAppFull(pid: Int, curAdj: Int): Int {
        if (CachedAppOptimizer.isOomAdjEnteredCached(curAdj)) {
            return if (compactApp(
                    pid,
                    CachedAppOptimizer.COMPACT_ACTION_FULL
                )
            ) ProcessCompactResultCode.success else ProcessCompactResultCode.problem
        }

        return ProcessCompactResultCode.doNothing
    }

    fun compactAppFullNoCheck(pid: Int): Int {
        return if (compactApp(
                pid,
                CachedAppOptimizer.COMPACT_ACTION_FULL
            )
        ) ProcessCompactResultCode.success else ProcessCompactResultCode.problem
    }

//    public boolean compactAppFull(ProcessInfo processInfo) {
//        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
//    }

    //    public boolean compactAppFull(ProcessInfo processInfo) {
    //        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
    //    }
    fun compactAppFull(processRecordKt: ProcessRecordKt): Int {
        return compactAppFull(processRecordKt.pid, processRecordKt.oomAdjScore)
    }

    fun compactAppFullNoCheck(processRecordKt: ProcessRecordKt): Int {
        return compactAppFullNoCheck(processRecordKt.pid)
    }

    /**
     * 进程压缩结果码
     *
     */
    class ProcessCompactResultCode {
        companion object {
            // 异常
            const val problem = -1

            // 正常执行
            const val success = 1

            // 未执行
            const val doNothing = 2

            // 无需执行(没有执行的必要)
            const val unNecessary = 3
        }
    }
}