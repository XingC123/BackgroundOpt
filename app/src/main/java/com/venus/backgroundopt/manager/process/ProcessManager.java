package com.venus.backgroundopt.manager.process;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.Process;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public class ProcessManager implements ILogger {
    /**
     * {@link android.os.Process#SIGNAL_USR1}
     */
    public static final int SIGNAL_10 = 10;

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
    private final AppMemoryTrimManager backgroundAppMemoryTrimManager = new BackgroundAppMemoryTrimManager();

    public void startBackgroundAppTrimTask(ProcessRecord processRecord) {
        // 移除前台任务
        cancelForegroundScheduledFuture(processRecord);

        backgroundAppMemoryTrimManager.startTrimTask(processRecord);
    }

    private final AppMemoryTrimManager foregroundAppMemoryTrimManager = new ForegroundAppMemoryTrimManager();

    public void startForegroundAppTrimTask(ProcessRecord processRecord) {
        // 移除后台任务
        cancelBackgroundScheduledFuture(processRecord);

        foregroundAppMemoryTrimManager.startTrimTask(processRecord);
    }

    public void cancelForegroundScheduledFuture(ProcessRecord processRecord) {
        foregroundAppMemoryTrimManager.cancelScheduledFuture(processRecord);
    }

    public void cancelBackgroundScheduledFuture(ProcessRecord processRecord) {
        backgroundAppMemoryTrimManager.cancelScheduledFuture(processRecord);
    }

    public void removeAllAppMemoryTrimTask(AppInfo appInfo) {
        foregroundAppMemoryTrimManager.removeTrimTask(appInfo.getmProcessRecord());
        backgroundAppMemoryTrimManager.removeTrimTask(appInfo.getmProcessRecord());
    }

    /**
     * 处理gc
     *
     * @param appInfo app信息
     */
    public void handleGC(AppInfo appInfo) {
        ProcessRecord processRecord = appInfo.getmProcessRecord();
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn(appInfo.getPackageName() + " processRecord为空设置个屁的gc");
            }

            return;
        }
        // kill -10 pid
        try {
            Process.sendSignal(appInfo.getmPid(), SIGNAL_10);

            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + " 触发gc, pid = " + appInfo.getmPid());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                getLogger().error(appInfo.getPackageName() + " 存在问题, 无法执行gc", e);
            }
        }
    }
}
