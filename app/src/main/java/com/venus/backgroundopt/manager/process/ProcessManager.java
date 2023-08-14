package com.venus.backgroundopt.manager.process;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.ProcessInfo;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.Process;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.utils.log.ILogger;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
        appCompactManager = new AppCompactManager(activityManagerService.getOomAdjuster().getCachedAppOptimizer());
    }

    /* *************************************************************************
     *                                                                         *
     * app压缩                                                                  *
     *                                                                         *
     **************************************************************************/
    private final AppCompactManager appCompactManager;

    /**
     * 添加压缩进程
     */
    public void addCompactProcessInfo(Collection<ProcessInfo> processInfos) {
        appCompactManager.addCompactProcessInfo(processInfos);
    }

    /**
     * 移除压缩进程
     * @param processInfo 进程信息
     */
    public void cancelCompactProcessInfo(@Nullable ProcessInfo processInfo) {
        appCompactManager.cancelCompactProcessInfo(processInfo);
    }

    public void cancelCompactProcessInfo(Collection<ProcessInfo> processInfos) {
        appCompactManager.cancelCompactProcessInfo(processInfos);
    }

    public void cancelCompactProcessInfo(AppInfo appInfo) {
        appCompactManager.cancelCompactProcessInfo(appInfo);
    }

    public void compactApp(ProcessRecord processRecord) {
        appCompactManager.compactApp(processRecord);
    }

    public void compactApp(int pid) {
        appCompactManager.compactApp(pid);
    }

    /**
     * 压缩app
     *
     * @param pid           进程pid
     * @param compactAction 压缩行为: {@link CachedAppOptimizer#COMPACT_ACTION_NONE}等
     */
    public void compactApp(int pid, int compactAction) {
        appCompactManager.compactApp(pid, compactAction);
    }

    /**
     * 部分压缩
     *
     * @param pid 要压缩的pid
     */
    public void compactAppSome(int pid) {
        appCompactManager.compactAppSome(pid);
    }

    public void compactAppSome(AppInfo appInfo) {
        appCompactManager.compactAppSome(appInfo);
    }

    /**
     * 全量压缩
     *
     * @param pid 要压缩的pid
     */
    public boolean compactAppFull(int pid, int curAdj) {
        return appCompactManager.compactAppFull(pid, curAdj);
    }

    public void compactAppFullNoCheck(int pid) {
        appCompactManager.compactAppFullNoCheck(pid);
    }

//    public boolean compactAppFull(ProcessInfo processInfo) {
//        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
//    }

    public boolean compactAppFull(ProcessInfo processInfo) {
        return appCompactManager.compactAppFull(processInfo);
    }

    public void compactAppFullNoCheck(ProcessInfo processInfo) {
        appCompactManager.compactAppFullNoCheck(processInfo);
    }

    /* *************************************************************************
     *                                                                         *
     * app内存回收管理                                                           *
     *                                                                         *
     **************************************************************************/
    private final AppMemoryTrimManagerKt appMemoryTrimManager = new AppMemoryTrimManagerKt();

    public void startBackgroundAppTrimTask(ProcessRecord processRecord) {
        appMemoryTrimManager.addBackgroundTask(processRecord);
    }

    public void startForegroundAppTrimTask(ProcessRecord processRecord) {
        appMemoryTrimManager.addForegroundTask(processRecord);
    }

    public void removeAllAppMemoryTrimTask(AppInfo appInfo) {
        appMemoryTrimManager.removeAllTask(appInfo.getmProcessRecord());
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
        handleGCNoNullCheck(processRecord);
    }

    public static void handleGC(ProcessRecord processRecord) {
        if (processRecord == null) {
            return;
        }

        handleGCNoNullCheck(processRecord);
    }

    public static void handleGCNoNullCheck(ProcessRecord processRecord) {
        // kill -10 pid
        try {
            Process.sendSignal(processRecord.getPid(), SIGNAL_10);

            if (BuildConfig.DEBUG) {
                ILogger.getLoggerStatic(ProcessManager.class)
                        .debug(processRecord.getPackageName() + " 触发gc, pid = " + processRecord.getPid());
            }
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                ILogger.getLoggerStatic(ProcessManager.class)
                        .error(processRecord.getPackageName() + " 存在问题, 无法执行gc", t);
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
        } catch (Throwable ignore) {
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
