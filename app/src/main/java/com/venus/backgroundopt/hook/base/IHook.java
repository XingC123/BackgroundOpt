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

        hook();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }

    public abstract void hook();
}
