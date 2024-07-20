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
                    
package com.venus.backgroundopt.xposed.manager.process;

import com.venus.backgroundopt.xposed.entity.android.android.content.ComponentCallbacks2;

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
    int getCorePoolSize() {
        return 4;
    }

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
        return 1;
    }

    @Override
    public long getTaskPeriod() {
        return 10;
    }

    @Override
    public TimeUnit getTaskTimeUnit() {
        return TimeUnit.MINUTES;
    }

    @Override
    public void runSpecialTask(AppMemoryTrimTask appMemoryTrimTask) {
        // App进入后台后, 执行gc
        appMemoryTrimTask.gcTask.scheduledFuture = executor.schedule(
                appMemoryTrimTask.gcTask.runnable,
                5,
                TimeUnit.MINUTES
        );
    }
}