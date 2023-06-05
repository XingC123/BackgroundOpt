package com.venus.backgroundopt.manager;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2;
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
public class AppMemoryTrimManager implements ILogger {
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(50);

    private final Map<ProcessRecord, ScheduledFuture<?>> processRecordScheduledFutureMap = new ConcurrentHashMap<>();

    public AppMemoryTrimManager() {
        // 在任务取消时一并将其移除
        executor.setRemoveOnCancelPolicy(true);
    }

    public void startTrimTask(ProcessRecord processRecord) {
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn("processRecord为空设置个屁");
            }

            return;
        }

        Runnable runnable = () -> {
            boolean result =
                    processRecord.scheduleTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE);

            if (BuildConfig.DEBUG) {
                String s;
                if (result) {
                    s = "成功";
                } else {
                    // 若调用scheduleTrimMemory()后目标进程被终结(kill), 则会得到此结果
                    s = "失败或未执行";

                    // 失败则移除此任务
                    removeTrimTask(processRecord);
                }

                getLogger().debug(processRecord.getPackageName() + ": 设置TrimMemoryTask ->>> " +
                        ComponentCallbacks2.TRIM_MEMORY_MODERATE + " " + s);
            }
        };

        // 立即执行。每隔3分钟执行
        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(runnable, 0, 10, TimeUnit.MINUTES);
        // 添加 进程-trim任务 映射
        processRecordScheduledFutureMap.put(processRecord, scheduledFuture);
    }

    public void removeTrimTask(ProcessRecord processRecord) {
        if (processRecord == null) {
            if (BuildConfig.DEBUG) {
                getLogger().warn("processRecord为空设置个屁");
            }
            return;
        }

        if (!processRecordScheduledFutureMap.containsKey(processRecord)) {
            return;
        }

        ScheduledFuture<?> scheduledFuture = processRecordScheduledFutureMap.get(processRecord);
        scheduledFuture.cancel(true);
        processRecordScheduledFutureMap.remove(processRecord);

        if (BuildConfig.DEBUG) {
            getLogger().debug("移除TrimMemoryTask ->>> " + processRecord.getPackageName());
        }
    }
}
