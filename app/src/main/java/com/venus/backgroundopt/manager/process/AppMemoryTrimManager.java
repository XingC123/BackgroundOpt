package com.venus.backgroundopt.manager.process;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 应用内存清理管理器
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/4
 */
public abstract class AppMemoryTrimManager implements ILogger {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(getCorePoolSize());

    private final Map<ProcessRecord, AppMemoryTrimTask> appMemoryTrimTaskMap = new ConcurrentHashMap<>();

    public AppMemoryTrimManager() {
        // 在任务取消时一并将其移除
        executor.setRemoveOnCancelPolicy(true);
    }

    abstract int getCorePoolSize();

    abstract int getDefaultTrimLevel();

    abstract String getMemoryTrimManagerName();

    abstract long getTaskInitialDelay();

    abstract long getTaskPeriod();

    abstract TimeUnit getTaskTimeUnit();

    abstract void runSpecialTask(AppMemoryTrimTask appMemoryTrimTask);

    String getMemoryTrimManagerNameImpl() {
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
                processRecord, AppMemoryTrimTask::new);

        // 移除原有任务
        cancelScheduledFuture(appMemoryTrimTask);  // 移除原有的以充分保持轮循间隔

        // 内存紧张任务立即执行。每隔3分钟执行
        appMemoryTrimTask.scheduleTrimMemoryTask.scheduledFuture = executor.scheduleAtFixedRate(
                appMemoryTrimTask.scheduleTrimMemoryTask.runnable,
                getTaskInitialDelay(),
                getTaskPeriod(),
                getTaskTimeUnit()
        );

        runSpecialTask(appMemoryTrimTask);
    }

    /**
     * 仅取消线程池中的任务
     * 在进程已设置过任务, 重新设置任务时调用
     */
    public void cancelScheduledFuture(AppMemoryTrimTask appMemoryTrimTask) {
        if (appMemoryTrimTask != null) {
            boolean scheduleTrimMemoryFlag = appMemoryTrimTask.scheduleTrimMemoryTask.cancelScheduledFuture();
            boolean gcFlag = appMemoryTrimTask.gcTask.cancelScheduledFuture();

            if (BuildConfig.DEBUG) {
                if (scheduleTrimMemoryFlag || gcFlag) { // 任一执行成功, 则打印
                    getLogger().debug(
                            getMemoryTrimManagerNameImpl()
                                    + appMemoryTrimTask.processRecord.getPackageName()
                                    + " ->>> 移除ScheduledFuture");
                }
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
            appMemoryTrimTask.clear();

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

    /**
     * App内存清理任务
     */
    class AppMemoryTrimTask {
        /**
         * 内存紧张任务
         */
        Task scheduleTrimMemoryTask;
        /**
         * app进行gc
         */
        Task gcTask;
        ProcessRecord processRecord;

        public AppMemoryTrimTask(ProcessRecord processRecord) {
            this.processRecord = processRecord;

            this.scheduleTrimMemoryTask = new Task(getAppMemoryTrimRunnable(processRecord));
            this.gcTask = new Task(() -> gcRunnable.accept(processRecord));
        }

        void clear() {
            scheduleTrimMemoryTask.clear();
            gcTask.clear();

            scheduleTrimMemoryTask = null;
            gcTask = null;
            processRecord = null;
        }

        static final Consumer<ProcessRecord> gcRunnable = ProcessManager::handleGC;

        class Task {
            Runnable runnable;
            ScheduledFuture<?> scheduledFuture;

            public Task(Runnable runnable) {
                this.runnable = runnable;
            }

            /**
             * 取消任务
             *
             * @return 若实际上取消了任务, 则返回true
             */
            boolean cancelScheduledFuture() {
                if (scheduledFuture == null) {
                    return false;
                }
                scheduledFuture.cancel(true);
                scheduledFuture = null;

                return true;
            }

            void clear() {
                cancelScheduledFuture();
                runnable = null;
            }
        }
    }
}
