package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.utils.log.ILogger;

/**
 * @author XingC
 * @date 2023/2/8
 * @version 1.0
 */
public abstract class AbstractHook implements ILogger {
    public ClassLoader classLoader;
    private RunningInfo runningInfo;

    private int lastHookTimes;

    public AbstractHook() {
    }

    public AbstractHook(ClassLoader classLoader, RunningInfo runningInfo) {
        this();
        this.classLoader = classLoader;
        this.runningInfo = runningInfo;
        hook();
    }

    public abstract HookPoint[] getHookPoint();

    public abstract void hook();

    public RunningInfo getHookInfo() {
        return runningInfo;
    }

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
