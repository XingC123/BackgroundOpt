package com.venus.backgroundopt.manager.process;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 应用内存清理管理器
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/4
 */
public abstract class AppMemoryTrimManager implements ILogger {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);

    private final Map<ProcessRecord, AppMemoryTrimTask> appMemoryTrimTaskMap = new ConcurrentHashMap<>();

    private static class AppMemoryTrimTask {
        Runnable runnable;
        ScheduledFuture<?> scheduledFuture;
        ProcessRecord processRecord;

        public AppMemoryTrimTask(ProcessRecord processRecord, Runnable runnable) {
            this.processRecord = processRecord;

            this.runnable = runnable;
        }
    }

    public AppMemoryTrimManager() {
        // 在任务取消时一并将其移除
        executor.setRemoveOnCancelPolicy(true);
        // 设置最大线程数(就目前来说, 只有从前台退往后台以后, 任务才会被添加于此, 安卓的后台, 真的能挂100个吗)
        executor.setMaximumPoolSize(100);
    }

    public abstract int getDefaultTrimLevel();

    public abstract String getMemoryTrimManagerName();

    public abstract long getTaskInitialDelay();

    public abstract long getTaskPeriod();

    public abstract TimeUnit getTaskTimeUnit();

    private String getMemoryTrimManagerNameImpl() {
        return getMemoryTrimManagerName() + ": ";
    }

    public void startTrimTask(ProcessRecord processRecord) {
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn("processRecord为空设置个屁");
            }

            return;
        }

        AppMemoryTrimTask appMemoryTrimTask = appMemoryTrimTaskMap.computeIfAbsent(
                processRecord,
                process -> new AppMemoryTrimTask(process, getAppMemoryTrimRunnable(process)));

        // 移除原有任务
        cancelScheduledFuture(appMemoryTrimTask);  // 移除原有的以充分保持轮循间隔

        // 立即执行。每隔3分钟执行
        appMemoryTrimTask.scheduledFuture = executor.scheduleAtFixedRate(
                appMemoryTrimTask.runnable,
                getTaskInitialDelay(),
                getTaskPeriod(),
                getTaskTimeUnit()
        );
    }

    /**
     * 仅取消线程池中的任务
     * 在进程已设置过任务, 重新设置任务时调用
     */
    public void cancelScheduledFuture(AppMemoryTrimTask appMemoryTrimTask) {
        if (appMemoryTrimTask != null && appMemoryTrimTask.scheduledFuture != null) {
            appMemoryTrimTask.scheduledFuture.cancel(true);
            appMemoryTrimTask.scheduledFuture = null;

            if (BuildConfig.DEBUG) {
                getLogger().debug(
                        getMemoryTrimManagerNameImpl()
                                + appMemoryTrimTask.processRecord.getPackageName()
                                + " ->>> 移除ScheduledFuture");
            }
        }
    }

    public void cancelScheduledFuture(ProcessRecord processRecord) {
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn(getMemoryTrimManagerNameImpl() + "processRecord为空cancelScheduledFuture不执行");
            }
            return;
        }


        if (appMemoryTrimTaskMap.containsKey(processRecord)) {
            cancelScheduledFuture(appMemoryTrimTaskMap.get(processRecord));
        }
    }

    /**
     * 移除进程的内存清理任务
     * 在进程被杀死时调用
     */
    public void removeTrimTask(ProcessRecord processRecord) {
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn(getMemoryTrimManagerNameImpl() + "processRecord为空移除个屁");
            }
            return;
        }

        AppMemoryTrimTask appMemoryTrimTask = appMemoryTrimTaskMap.remove(processRecord);

        if (appMemoryTrimTask != null) {
            appMemoryTrimTask.runnable = null;
            cancelScheduledFuture(appMemoryTrimTask);

            if (BuildConfig.DEBUG) {
                getLogger().debug(getMemoryTrimManagerNameImpl() + "移除TrimMemoryTask ->>> "
                        + processRecord.getPackageName());
            }
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().debug(getMemoryTrimManagerNameImpl() + "移除TrimMemoryTask ->>> "
                        + processRecord.getPackageName() + " 无需移除");
            }
        }
    }

    private Runnable getAppMemoryTrimRunnable(ProcessRecord processRecord) {
        return () -> {
            boolean result =
                    processRecord.scheduleTrimMemory(getDefaultTrimLevel());

            if (result) {
                if (BuildConfig.DEBUG) {
                    getLogger().debug(
                            getMemoryTrimManagerNameImpl() + processRecord.getPackageName()
                                    + ": 设置TrimMemoryTask ->>> "
                                    + getDefaultTrimLevel() + " 成功");
                }
            } else {    // 若调用scheduleTrimMemory()后目标进程被终结(kill), 则会得到此结果
                // 失败则移除此任务
                removeTrimTask(processRecord);

                getLogger().warn(
                        getMemoryTrimManagerNameImpl() + processRecord.getPackageName()
                                + ": 设置TrimMemoryTask ->>> "
                                + getDefaultTrimLevel() + " 失败或未执行");
            }
        };
    }
}