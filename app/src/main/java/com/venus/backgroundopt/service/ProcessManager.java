package com.venus.backgroundopt.service;

import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public class ProcessManager {
    private static final int DEFAULT_COMPACT_LEVEL = CachedAppOptimizer.COMPACT_ACTION_ANON;

    private final CachedAppOptimizer cachedAppOptimizer;

    public ProcessManager(ActivityManagerService activityManagerService) {
        this.cachedAppOptimizer = activityManagerService.getOomAdjuster().getCachedAppOptimizer();
    }

    public void compactApp(ProcessRecord processRecord) {
        compactApp(processRecord.getPid());
    }

    public void compactApp(int pid) {
        compactApp(pid, DEFAULT_COMPACT_LEVEL);
    }

    public void compactApp(int pid, int compactAction) {
        cachedAppOptimizer.compactProcess(pid, compactAction);
    }
}
