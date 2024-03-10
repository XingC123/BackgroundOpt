/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
                    
 package com.venus.backgroundopt.manager.process;

import static com.venus.backgroundopt.core.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.Process;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt;
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtilsKt;
import com.venus.backgroundopt.utils.log.ILogger;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

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

    private final RunningInfo runningInfo;

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

    public ProcessManager(RunningInfo runningInfo) {
        this.runningInfo = runningInfo;
        ActivityManagerService activityManagerService = runningInfo.getActivityManagerService();
        /*appCompactManager = new AppCompactManager(
                activityManagerService.getOomAdjuster().getCachedAppOptimizer(),
                runningInfo
        );*/
        appCompactManager2 = new AppCompactManager2(activityManagerService.getOomAdjuster().getCachedAppOptimizer(),
                runningInfo);
        appMemoryTrimManager = new AppMemoryTrimManagerKt(runningInfo);
    }

    /* *************************************************************************
     *                                                                         *
     * app压缩                                                                  *
     *                                                                         *
     **************************************************************************/
    private AppCompactManager appCompactManager;
    private AppCompactManager2 appCompactManager2;

    public Set<ProcessRecordKt> getCompactProcessInfos() {
        return appCompactManager.getCompactProcesses();
    }

    public void setAutoStopCompactTask(boolean enable) {
        appCompactManager.setAutoStopCompactTask(enable);
    }

    /**
     * 添加压缩进程
     */
    private void addCompactProcess(AppInfo appInfo) {
        appCompactManager.addCompactProcess(appInfo);
    }

    public void addCompactProcess(ProcessRecordKt processRecord) {
        appCompactManager.addCompactProcess(processRecord);
    }

    /**
     * 移除压缩进程
     *
     * @param processRecord 进程记录
     */
    public void cancelCompactProcess(@Nullable ProcessRecordKt processRecord) {
        appCompactManager.cancelCompactProcess(processRecord);
    }

    private void cancelCompactProcess(AppInfo appInfo) {
        appCompactManager.cancelCompactProcess(appInfo);
    }

    public void compactApp(ProcessRecordKt processRecord) {
        appCompactManager.compactApp(processRecord);
    }

    public void compactApp(int pid) {
        appCompactManager.compactApp(pid);
    }

    public void compactProcess(ProcessRecordKt processRecordKt, int lastOomScoreAdj, int curOomScoreAdj, int oomAdjustLevel) {
        appCompactManager2.compactProcess(processRecordKt, lastOomScoreAdj, curOomScoreAdj, oomAdjustLevel);
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
    public int compactAppFull(int pid, int curAdj) {
        return appCompactManager.compactAppFull(pid, curAdj);
    }

    public void compactAppFullNoCheck(int pid) {
        appCompactManager.compactAppFullNoCheck(pid);
    }

//    public boolean compactAppFull(ProcessInfo processInfo) {
//        return cachedAppOptimizer.compactApp(processInfo.getProcessRecord(), true, "Full");
//    }

    public int compactAppFull(ProcessRecordKt processRecord) {
        return appCompactManager.compactAppFull(processRecord);
    }

    public void compactAppFullNoCheck(ProcessRecordKt processRecord) {
        appCompactManager.compactAppFullNoCheck(processRecord);
    }

    /* *************************************************************************
     *                                                                         *
     * app内存回收管理                                                           *
     *                                                                         *
     **************************************************************************/
    private final AppMemoryTrimManagerKt appMemoryTrimManager;

    public void configureForegroundTrimCheckTask(boolean isEnable) {
        appMemoryTrimManager.setEnableForegroundTrim(isEnable);
    }

    public Set<ProcessRecordKt> getForegroundTasks() {
        return appMemoryTrimManager.getForegroundTasks();
    }

    public Set<ProcessRecordKt> getBackgroundTasks() {
        return appMemoryTrimManager.getBackgroundTasks();
    }

    private void startBackgroundAppTrimTask(ProcessRecordKt processRecord) {
        appMemoryTrimManager.addBackgroundTask(processRecord);
    }

    private void startForegroundAppTrimTask(ProcessRecordKt processRecord) {
        appMemoryTrimManager.addForegroundTask(processRecord);
    }

    private void removeAllAppMemoryTrimTask(AppInfo appInfo) {
        appMemoryTrimManager.removeAllTask(appInfo.getmProcessRecord());
    }

    /**
     * 处理gc
     *
     * @param appInfo app信息
     */
    public void handleGC(AppInfo appInfo) {
        ProcessRecordKt processRecord = appInfo.getmProcessRecord();
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn(appInfo.getPackageName() + " processRecord为空设置个屁的gc");
            }

            return;
        }
        handleGCNoNullCheck(processRecord);
    }

    public static boolean handleGC(ProcessRecordKt processRecord) {
        if (processRecord == null) {
            return false;
        }

        return handleGCNoNullCheck(processRecord);
    }

    public static boolean handleGCNoNullCheck(ProcessRecordKt processRecord) {
        // kill -10 pid
        try {
            Process.sendSignal(processRecord.getPid(), SIGNAL_10);

            if (BuildConfig.DEBUG) {
                ILogger.getLoggerStatic(ProcessManager.class)
                        .debug(processRecord.getPackageName() + " 触发gc, pid = " + processRecord.getPid());
            }
            return true;
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {
                ILogger.getLoggerStatic(ProcessManager.class)
                        .error(processRecord.getPackageName() + " 存在问题, 无法执行gc", t);
            }
        }

        return false;
    }

    /* *************************************************************************
     *                                                                         *
     * 统一调度处                                                                *
     *                                                                         *
     **************************************************************************/
    public void appActive(AppInfo appInfo) {
        ConcurrentUtilsKt.lock(appInfo, () -> {
            // 移除压缩任务
            // cancelCompactProcess(appInfo);
            // 添加前台任务
            startForegroundAppTrimTask(appInfo.getmProcessRecord());
            return null;
        });
    }

    public void appIdle(AppInfo appInfo) {
        ConcurrentUtilsKt.lock(appInfo, () -> {
            // 添加后台任务
            startBackgroundAppTrimTask(appInfo.getmProcessRecord());
            // 添加压缩任务
            // addCompactProcess(appInfo);
            return null;
        });
    }

    public void appDie(AppInfo appInfo) {
        // 移除前后台任务
        removeAllAppMemoryTrimTask(appInfo);
        // 取消内存压缩任务
        // cancelCompactProcess(appInfo);
    }

    /**
     * 设置app到后台进程组
     *
     * @param appInfo app信息
     */
    public void setAppToBackgroundProcessGroup(AppInfo appInfo) {
        if (appInfo == null) {
            return;
        }
        Set<Integer> processInfoPids = runningInfo.getRunningProcesses().stream()
                .filter(processRecordKt -> processRecordKt.getAppInfo() == appInfo)
                .map(BaseProcessInfoKt::getPid)
                .collect(Collectors.toSet());

        if (processInfoPids.isEmpty()) {
            return;
        }

        processInfoPids.forEach(this::setPidToBackgroundProcessGroup);

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