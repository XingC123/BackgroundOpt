package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.interfaces.ILogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public Object[] getArgs() {
        // 处理hook方法的参数类型
        List<Object> params = new ArrayList<>(Arrays.asList(getTargetParam()));
        params.add(getActionMethod());  // 将动作方法添加到hook方法传参数类型中

        return params.toArray();
    }

    public abstract String getTargetClass();

    public abstract String getTargetMethod();

    public abstract XC_MethodHook getActionMethod();

    public abstract Object[] getTargetParam();

    public abstract void hook();
}
