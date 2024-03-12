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

package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.utils.log.ILogger;

/**
 * @author XingC
 * @date 2023/11/20
 */
public abstract class IHook implements ILogger {
    ClassLoader classLoader;

    RunningInfo runningInfo;

    public IHook(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public IHook(ClassLoader classLoader, RunningInfo runningInfo) {
        this.classLoader = classLoader;
        this.runningInfo = runningInfo;

        try {
            hook();
        } catch (Throwable throwable) {
            getLogger().error("hook失败", throwable);
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }

    public abstract void hook();
}