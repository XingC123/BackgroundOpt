package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.ProcessInfo
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.log.ILogger
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
    private var cachedAppOptimizer: CachedAppOptimizer
) : ILogger {
    companion object {
        // 默认压缩级别
        const val DEFAULT_COMPACT_LEVEL = CachedAppOptimizer.COMPACT_ACTION_ANON

        // App压缩扫描线程池配置
        const val initialDelay = 10L
        const val delay = 10L
        val timeUnit = TimeUnit.MINUTES
    }

    // App压缩扫描线程
    private val executor = ScheduledThreadPoolExecutor(1)
    private val compactAppScheduledFutures = ConcurrentHashMap<AppInfo, ScheduledFuture<*>>()

    init {
        executor.removeOnCancelPolicy = true
    }

    /* *************************************************************************
     *                                                                         *
     * 压缩任务处理                                                               *
     *                                                                         *
     **************************************************************************/
    /**
     * 取消app压缩
     *
     * @param appInfo 应用信息
     */
    fun cancelAppCompact(appInfo: AppInfo) {
        compactAppScheduledFutures.remove(appInfo)?.let {
            it.cancel(true)
            if (BuildConfig.DEBUG) {
                logger.debug("包名: ${appInfo.packageName}, uid: ${appInfo.uid} >>> 移除自待压缩列表")
            }
        }
    }

    /**
     * 添加内存压缩任务
     *
     * @param appInfo 应用信息
     */
    fun addCompactApp(appInfo: AppInfo) {
        compactAppScheduledFutures[appInfo] = executor.schedule({
            appInfo.processInfos.forEach { processInfo ->
                val oomAdjEnteredCached = processInfo.oomAdjScore >= ProcessList.CACHED_APP_MIN_ADJ
                        && processInfo.oomAdjScore <= ProcessList.CACHED_APP_MAX_ADJ
                if (oomAdjEnteredCached) {
                    try {
                        compactAppFullNoCheck(processInfo)

                        if (BuildConfig.DEBUG) {
                            logger.debug("包名: ${appInfo.packageName}, uid: ${processInfo.uid}, pid: ${processInfo.pid} >>> 因[oom_score]而内存压缩")
                        }
                    } catch (t: Throwable) {
                        logger.warn(
                            "包名: ${appInfo.packageName}, uid: ${processInfo.uid}, pid: ${processInfo.pid} >>> 因[oom_score]而内存压缩, 压缩失败",
                            t
                        )
                    } finally {
                        // 压缩完毕/出错后 移除
                        cancelAppCompact(appInfo)
                    }
                }
            }
        }, delay, timeUnit)

        if (BuildConfig.DEBUG) {
            logger.debug("包名: ${appInfo.packageName}, uid: ${appInfo.uid} >>> 加入待压缩列表")
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 压缩方法                                                                  *
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
    fun compactAppFull(pid: Int, curAdj: Int) {
        if (CachedAppOptimizer.isOomAdjEnteredCached(curAdj)) {
            compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL)
        }
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
    fun compactAppFull(processInfo: ProcessInfo, curAdj: Int) {
        compactAppFull(processInfo.pid, curAdj)
    }

    fun compactAppFullNoCheck(processInfo: ProcessInfo) {
        compactAppFullNoCheck(processInfo.pid)
    }
}