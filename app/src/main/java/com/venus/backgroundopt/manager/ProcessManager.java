package com.venus.backgroundopt.manager;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public class ProcessManager implements ILogger {
    public ProcessManager(ActivityManagerService activityManagerService) {
        this.cachedAppOptimizer = activityManagerService.getOomAdjuster().getCachedAppOptimizer();
    }

    /* *************************************************************************
     *                                                                         *
     * app压缩                                                                  *
     *                                                                         *
     **************************************************************************/
    // 默认压缩级别
    private static final int DEFAULT_COMPACT_LEVEL = CachedAppOptimizer.COMPACT_ACTION_ANON;

    // 封装的CachedAppOptimizer
    private final CachedAppOptimizer cachedAppOptimizer;

    public void compactApp(ProcessRecord processRecord) {
        compactApp(processRecord.getPid());
    }

    public void compactApp(int pid) {
        compactApp(pid, DEFAULT_COMPACT_LEVEL);
    }

    /**
     * 压缩app
     *
     * @param pid           进程pid
     * @param compactAction 压缩行为: {@link CachedAppOptimizer#COMPACT_ACTION_NONE}等
     */
    public void compactApp(int pid, int compactAction) {
        cachedAppOptimizer.compactProcess(pid, compactAction);
    }

    /* *************************************************************************
     *                                                                         *
     * app内存回收管理                                                           *
     *                                                                         *
     **************************************************************************/
    AppMemoryTrimManager appMemoryTrimManager = new AppMemoryTrimManager();

    public void startTrimTask(ProcessRecord processRecord) {
        appMemoryTrimManager.startTrimTask(processRecord);
    }

    public void removeTrimTask(ProcessRecord processRecord) {
        appMemoryTrimManager.removeTrimTask(processRecord);
    }
}
