package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.entity.RunningInfo;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/25
 */
public abstract class ConstructorHook extends MethodHook {
    public ConstructorHook(ClassLoader classLoader) {
        super(classLoader);
    }

    public ConstructorHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetMethod() {
        return null;
    }

    @Override
    public void hook() {
        try {
            XposedHelpers.findAndHookConstructor(
                    getTargetClass(),
                    classLoader,
                    getArgs()
            );
            debugLog(isDebugMode() &&
                    getLogger().debug("[" + getTargetClass() + "构造器" + "]hook成功"));
        } catch (Exception e) {
            debugLog(isDebugMode() &&
                    getLogger().debug("[" + getTargetClass() + "构造器" + "]hook失败", e));
        }
    }
}
