package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.interfaces.ILogger;

import de.robv.android.xposed.XC_MethodHook;

/**
 * author: XingC
 * date: 2023/2/8
 * version: 1.0
 */
public abstract class AbstractHook implements ILogger {
    public ClassLoader classLoader;
    private RunningInfo runningInfo;

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }

    public AbstractHook() {
    }

    public AbstractHook(ClassLoader classLoader, RunningInfo runningInfo) {
        this();
        this.classLoader = classLoader;
        this.runningInfo = runningInfo;
        hook();
    }

    public abstract String getTargetClass();

    public abstract String getTargetMethod();

    public abstract XC_MethodHook getActionMethod();

    public abstract void hook();
}
