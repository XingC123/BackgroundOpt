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
            if (hookPoint instanceof IneffectiveHookPoint) {
                getLogger().debug("因不满足条件, [" + hookPoint.getClassName() + "." + hookPoint.getMethodName() + "]不进行hook");
            } else {
                hookPoint.hook(classLoader, HookPoint.MethodType.Member);
            }
        }
    }

    private static final HookPoint[] defaultHookPoint = new HookPoint[0];

    public HookPoint[] getConstructorHookPoint() {
        return defaultHookPoint;
    }
}
