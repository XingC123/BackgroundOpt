package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.annotation.UsageComment
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.concurrent.ConcurrentHashSet
import com.venus.backgroundopt.utils.concurrent.visualSynchronize
import com.venus.backgroundopt.utils.log.ILogger
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 应用内存压缩管理器
 * @author XingC
 * @date 2023/8/8
 */
class AppCompactManager(// 封装的CachedAppOptimizer
    private var cachedAppOptimizer: CachedAppOptimizer
) : ILogger {
    companion object {
        // 默认压缩级别
        const val DEFAULT_COMPACT_LEVEL = CachedAppOptimizer.COMPACT_ACTION_ANON

        // 是否启用"压缩成功则移除进程" + "无内存压缩任务则关闭轮循"
        // 暂只能从源码层修改, ui未提供开关
        const val autoStopCompactCheck = false

        // App压缩扫描线程池配置
        const val initialDelay = 0L
        const val delay = 15L
        val timeUnit = TimeUnit.MINUTES
    }

    // App压缩处理线程池
    private val executor = ScheduledThreadPoolExecutor(1).apply {
        removeOnCancelPolicy = true
    }

    // 待压缩的进程信息列表
    // 注意: 是"进程信息"而不是"应用信息", 其size代表的是进程数不是app数
    val compactProcesses = ConcurrentHashSet<ProcessRecordKt>()

    init {
        // 如果此项未启用, 则直接启动轮循任务
        logger.info("压缩任务自动关闭: $autoStopCompactCheck")
        if (!autoStopCompactCheck) {
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
            compactProcesses.forEach {
                it.appInfo.visualSynchronize {
                    // 根据默认规则压缩
                    var compactMethod: (processRecordKt: ProcessRecordKt) -> Boolean =
                        ::compactAppFull
                    val mainProcess = it.mainProcess

                    if (mainProcess) {
                        it.removeIfRedundant(compactProcesses)
                    }
                    if (it.mainProcess || upgradeSubProcessNames.contains(it.processName)) {
                        val currentTime = System.currentTimeMillis()
                        if (!it.isAllowedCompact(currentTime)) {  // 若压缩间隔不满足, 则跳过等待下一轮
                            return@visualSynchronize
                        }
                        // 压缩条件由ProcessInfo.isAllowedCompact判断。因此, 符合条件后直接调用压缩
                        compactMethod = ::compactAppFullNoCheck

                        it.setLastCompactTime(currentTime)
                    }
                    /*
                        result: 0 -> 异常
                                1 -> 成功
                                2 -> 未执行
                     */
                    var result = 2
                    try {
                        result = if (compactMethod(it)) 1 else 2
                        if (BuildConfig.DEBUG) {
                            if (result == 1) {
                                logger.debug("uid: ${it.uid}, pid: ${it.pid} >>> 因[OOM_SCORE] 而内存压缩")
                            }
                        }
                    } catch (t: Throwable) {
                        result = 0
                        if (BuildConfig.DEBUG) {
                            logger.warn(
                                "uid: ${it.uid}, pid: ${it.pid} >>> 因[OOM_SCORE] 而内存压缩: 发生异常!压缩执行失败或进程已停止",
                                t
                            )
                        }
                    } finally {
                        if (result == 1 || result == 0) {
                            // 若启用自动停止 或 执行异常, 则进行移除
                            if (autoStopCompactCheck || result == 0) {
                                cancelCompactProcess(it)
                            }
                            compactCount++
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                logger.debug("内存压缩任务检查完毕。本次压缩了[${compactCount}]个进程, 列表还剩[${compactProcesses.size}]个进程")
            }
        }, initialDelay, delay, timeUnit)

        compactScheduledFuture?.let {
            if (BuildConfig.DEBUG) {
                logger.debug("创建内存压缩检查任务成功")
            }
        }
    }

    private fun checkCompactTask() {
        if (autoStopCompactCheck) {
            compactScheduledFuture?.let {
                if (compactProcesses.size == 0) {    // 若此时没有待压缩任务, 则取消检查任务
                    it.cancel(true)
                    compactScheduledFuture = null

                    if (BuildConfig.DEBUG) {
                        logger.debug("待压缩列表为空, 停止检查任务")
                    }
                }
            } ?: startCompactTask() // 需要待压缩任务, 创建
        }
    }

    /**
     * 添加压缩进程
     */
    fun addCompactProcess(appInfo: AppInfo) {
        appInfo.visualSynchronize {
            val processRecordKts = appInfo.processes
            if (processRecordKts.isNotEmpty()) {
                processRecordKts.forEach { addCompactProcessImpl(it) }
                checkCompactTask()

                if (BuildConfig.DEBUG) {
                    logger.debug("uid: ${processRecordKts.first().uid} >>> [批量]加入待压缩列表")
                }
            }
        }
    }

    fun addCompactProcess(processRecordKt: ProcessRecordKt) {
        processRecordKt.appInfo.visualSynchronize {
            addCompactProcessImpl(processRecordKt)
            checkCompactTask()

            if (BuildConfig.DEBUG) {
                logger.debug("uid: ${processRecordKt.uid}, pid: ${processRecordKt.pid} >>> 加入待压缩列表")
            }
        }
    }

    /**
     * 在确保已对[AppInfo]上锁的情况下再使用
     *
     * @param processRecordKt
     */
    @UsageComment("确保AppInfo已上锁")
    private fun addCompactProcessImpl(processRecordKt: ProcessRecordKt) {
        compactProcesses.add(processRecordKt.also { it.setLastCompactTime(System.currentTimeMillis()) })
    }

    /**
     * 移除压缩进程
     */
    fun cancelCompactProcess(processRecordKt: ProcessRecordKt?) {
        processRecordKt?.let { process ->
            process.visualSynchronize {
                compactProcesses.remove(processRecordKt).also {
                    if (it) {
                        checkCompactTask()

                        if (BuildConfig.DEBUG) {
                            logger.debug("uid: ${process.uid}, pid: ${process.pid} >>> 移除自待压缩列表")
                        }
                    }
                }
            }
        }
    }

    fun cancelCompactProcess(appInfo: AppInfo) {
        appInfo.visualSynchronize {
            val set = compactProcesses.filter { it.uid == appInfo.uid }.toSet()
            if (set.isNotEmpty()) {
                compactProcesses.removeAll(set.toSet()).let {
                    if (it) {
                        checkCompactTask()

                        if (BuildConfig.DEBUG) {
                            logger.debug("uid: ${set.first().uid} >>> 移除自待压缩列表")
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
    fun compactApp(pid: Int, compactAction: Int) {
        cachedAppOptimizer.compactProcess(pid, compactAction)
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
     */
    fun compactAppFull(pid: Int, curAdj: Int): Boolean {
        if (CachedAppOptimizer.isOomAdjEnteredCached(curAdj)) {
            compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL)
            return true
        }

        return false
    }

    fun compactAppFullNoCheck(pid: Int): Boolean {
        compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL)
        return true
    }

//    public boolean compactAppFull(ProcessInfo processInfo) {
//        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
//    }

    //    public boolean compactAppFull(ProcessInfo processInfo) {
    //        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
    //    }
    fun compactAppFull(processRecordKt: ProcessRecordKt): Boolean {
        return compactAppFull(processRecordKt.pid, processRecordKt.oomAdjScore)
    }

    fun compactAppFullNoCheck(processRecordKt: ProcessRecordKt): Boolean {
        return compactAppFullNoCheck(processRecordKt.pid)
    }
}