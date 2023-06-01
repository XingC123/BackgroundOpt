package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.entity.RunningInfo;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class MethodHook extends AbstractHook {
    public MethodHook(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public MethodHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public void hook() {
        for (HookPoint hookPoint : getConstructorHookPoint()) {
            hookPoint.hook(classLoader, HookPoint.MethodType.Constructor);
        }

        for (HookPoint hookPoint : getHookPoint()) {
            hookPoint.hook(classLoader, HookPoint.MethodType.Member);
        }
    }

    public HookPoint[] getConstructorHookPoint() {
        return new HookPoint[0];
    }
}
