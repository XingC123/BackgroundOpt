package com.venus.backgroundopt.manager.process;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.ProcessInfo;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.Process;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

import java.util.Set;

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

    /* *************************************************************************
     *                                                                         *
     * thread group                                                            *
     *                                                                         *
     **************************************************************************/
    /**
     * 偏移量
     */
    public static final int THREAD_GROUP_LEVEL_OFFSET = 2 * Process.THREAD_GROUP_RESTRICTED;

    /**
     * 后台组
     */
    public static final int THREAD_GROUP_BACKGROUND = Process.THREAD_GROUP_BACKGROUND + THREAD_GROUP_LEVEL_OFFSET;

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

    /**
     * 部分压缩
     *
     * @param pid 要压缩的pid
     */
    public void compactAppSome(int pid) {
        compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FILE);

        if (BuildConfig.DEBUG) {
            getLogger().debug("pid: " + pid + " >>> 进行了一次compactAppSome");
        }
    }

    public void compactAppSome(AppInfo appInfo) {
        appInfo.getProcessInfoPids().parallelStream().forEach(this::compactAppSome);
    }

    /**
     * 全量压缩
     *
     * @param pid 要压缩的pid
     */
    public void compactAppFull(int pid, int curAdj) {
        if (CachedAppOptimizer.isOomAdjEnteredCached(curAdj)) {
            compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL);
        }
    }

    public void compactAppFullNoCheck(int pid) {
        compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL);
    }

//    public boolean compactAppFull(ProcessInfo processInfo) {
//        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
//    }

    public void compactAppFull(ProcessInfo processInfo, int curAdj) {
        compactAppFull(processInfo.getPid(), curAdj);
    }

    public void compactAppFullNoCheck(ProcessInfo processInfo) {
        compactAppFullNoCheck(processInfo.getPid());
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

    /**
     * 设置app到后台进程组
     *
     * @param appInfo app信息
     */
    public void setAppToBackgroundProcessGroup(AppInfo appInfo) {
        Set<Integer> processInfoPids;
        if (appInfo == null || (processInfoPids = appInfo.getProcessInfoPids()) == null) {
            return;
        }

        processInfoPids.parallelStream().forEach(this::setPidToBackgroundProcessGroup);

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + " 进行进程组设置 >>> THREAD_GROUP_BACKGROUND");
        }
    }

    /**
     * 设置给定pid到后台进程组
     *
     * @param pid 要设置的pid
     */
    public void setPidToBackgroundProcessGroup(int pid) {
        try {
            int processGroup = Process.getProcessGroup(pid);
            if (processGroup == Process.THREAD_GROUP_AUDIO_APP ||
                    processGroup == Process.THREAD_GROUP_AUDIO_SYS) {
                return;
            }
            Process.setProcessGroup(pid, THREAD_GROUP_BACKGROUND);
        } catch (Exception ignore) {
            // 不进行任何设置
        }
    }

    /**
     * 当appInfo处于tmp或idle分组时, 设置给定pid到后台进程组
     *
     * @param pid     要设置的pid
     * @param appInfo pid所属app
     */
    public void setPidToBackgroundProcessGroup(int pid, AppInfo appInfo) {
        if (appInfo != null && appInfo.getAppGroupEnum() == AppGroupEnum.IDLE) {
            setPidToBackgroundProcessGroup(pid);
        }
    }
}
