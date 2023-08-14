package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.ProcessInfo
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.log.ILogger
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
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

        // App压缩扫描线程池配置
        const val initialDelay = 0L
        const val delay = 10L
        val timeUnit = TimeUnit.MINUTES
    }

    // App压缩处理线程池
    private val executor = ScheduledThreadPoolExecutor(1)
    private val compactProcessInfos = Collections.newSetFromMap<ProcessInfo>(ConcurrentHashMap())

    init {
        executor.scheduleWithFixedDelay({
            var compactCount = 0
            compactProcessInfos.forEach {
                /*
                    result: 0 -> 异常
                            1 -> 成功
                            2 -> 未执行
                 */
                var result = 2
                try {
                    val flag = compactAppFull(it)
                    result = if (flag) 1 else 2
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
                        cancelCompactProcessInfo(it)
                        compactCount++
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                logger.debug("内存压缩任务检查完毕。本次压缩了[${compactCount}]个进程, 列表还剩[${compactProcessInfos.size}]个进程")
            }
        }, initialDelay, delay, timeUnit)
    }

    /* *************************************************************************
     *                                                                         *
     * 压缩任务处理                                                               *
     *                                                                         *
     **************************************************************************/
    /**
     * 添加压缩进程
     */
    fun addCompactProcessInfo(processInfos: Collection<ProcessInfo>) {
        if (processInfos.isNotEmpty()) {
            compactProcessInfos.addAll(processInfos)
            if (BuildConfig.DEBUG) {
                logger.debug("uid: ${processInfos.first().uid} >>> 加入待压缩列表")
            }
        }
    }

    /**
     * 移除压缩进程
     */
    fun cancelCompactProcessInfo(processInfos: Collection<ProcessInfo>) {
        if (processInfos.isNotEmpty()) {
            compactProcessInfos.removeAll(processInfos.toSet()).let {
                if (BuildConfig.DEBUG) {
                    if (it) {
                        logger.debug("uid: ${processInfos.first().uid} >>> 移除自待压缩列表")
                    }
                }
            }
        }
    }

    fun cancelCompactProcessInfo(processInfo: ProcessInfo?) {
        processInfo?.let {
            compactProcessInfos.remove(processInfo).let {
                if (BuildConfig.DEBUG) {
                    if (it) {
                        logger.debug("uid: ${processInfo.uid}, pid: ${processInfo.pid} >>> 移除自待压缩列表")
                    }
                }
            }
        }
    }

    fun cancelCompactProcessInfo(appInfo: AppInfo) {
        val set = compactProcessInfos.filter { it.uid == appInfo.uid }.toSet()
        cancelCompactProcessInfo(set)
    }

    /* *************************************************************************
     *                                                                         *
     * 压缩方式                                                                  *
     *                                                                         *
     **************************************************************************/
    fun compactApp(processRecord: ProcessRecord) {
        compactApp(processRecord.pid)
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
        appInfo.processInfoPids.forEach { pid: Int ->
            this.compactAppSome(pid)
        }
    }

    /**
     * 全量压缩
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

    fun compactAppFullNoCheck(pid: Int) {
        compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL)
    }

//    public boolean compactAppFull(ProcessInfo processInfo) {
//        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
//    }

    //    public boolean compactAppFull(ProcessInfo processInfo) {
    //        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
    //    }
    fun compactAppFull(processInfo: ProcessInfo): Boolean {
        return compactAppFull(processInfo.pid, processInfo.oomAdjScore)
    }

    fun compactAppFullNoCheck(processInfo: ProcessInfo) {
        compactAppFullNoCheck(processInfo.pid)
    }
}