package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.ProcessInfo
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.utils.log.ILogger

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
//        const val initialDelay = 0L
//        const val delay = 10L
//        val timeUnit = TimeUnit.MINUTES
    }

//    // App压缩扫描线程
//    private val executor = ScheduledThreadPoolExecutor(1)
//    private val compactApps = Collections.newSetFromMap<AppInfo>(ConcurrentHashMap())
//
//    init {
//        executor.scheduleWithFixedDelay({
//            compactApps.forEach { appInfo ->
//                appInfo.processInfos.forEach { processInfo ->  // 压缩当前进程
//                    val curAppGroupEnum: AppGroupEnum = appInfo.appGroupEnum
//                    val lastAppGroupEnum: AppGroupEnum? = processInfo.lastAppGroupEnum
//
//                    if (lastAppGroupEnum != null && curAppGroupEnum != lastAppGroupEnum) {  // app经历了idle->active->idle
//                        compactAppFullNoCheck(processInfo)
//                        if (BuildConfig.DEBUG) {
//                            logger.debug("包名: " + appInfo.packageName + ", uid: " + appInfo.uid + "的pid: " + processInfo.pid + " >>> 因[所在app内存状态改变]而内存压缩")
//                        }
//                    } else  // 参照了com.android.server.am.CachedAppOptimizer.void onOomAdjustChanged(int oldAdj, int newAdj, ProcessRecord app)
//                        if (processInfo.oomAdjScore < ProcessList.CACHED_APP_MIN_ADJ && oomAdjScore >= ProcessList.CACHED_APP_MIN_ADJ && oomAdjScore <= ProcessList.CACHED_APP_MAX_ADJ) {
//                            val currentTimeMillis = System.currentTimeMillis()
//                            if (processInfo.isAllowedCompact(currentTimeMillis)) {
//                                compactAppFullNoCheck(processInfo)
//                                processInfo.setLastCompactTime(currentTimeMillis)
//                                if (BuildConfig.DEBUG) {
//                                    logger.debug("包名: " + appInfo.packageName + ", uid: " + appInfo.uid + "的pid: " + processInfo.pid + " >>> 因[oom_score]而内存压缩")
//                                }
//                            }
//                        }
//                }
//            }
//        }, initialDelay, delay, timeUnit)
//    }

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