package com.venus.backgroundopt.manager.process;

import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2;

import java.util.concurrent.TimeUnit;

/**
 * 后台app的内存清理管理器
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/10
 */
public class BackgroundAppMemoryTrimManager extends AppMemoryTrimManager {
    @Override
    public int getDefaultTrimLevel() {
        return ComponentCallbacks2.TRIM_MEMORY_MODERATE;
    }

    @Override
    public String getMemoryTrimManagerName() {
        return "BackgroundAppMemoryTrimManager";
    }

    @Override
    public long getTaskInitialDelay() {
        return 0;
    }

    @Override
    public long getTaskPeriod() {
        return 10;
    }

    @Override
    public TimeUnit getTaskTimeUnit() {
        return TimeUnit.MINUTES;
    }
}
