package com.venus.backgroundopt.hook.handle.android;

import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.DoNotingHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class PhantomProcessListHook extends MethodHook {
    public PhantomProcessListHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[] {
                new HookPoint(
                        ClassConstants.PhantomProcessList,
                        MethodConstants.trimPhantomProcessesIfNecessary,
                        new HookAction[]{
                                new DoNotingHookAction()
                        }
                ),
        };
    }

    private Object handleTrimPhantomProcessesIfNecessary(XC_MethodHook.MethodHookParam param) {
        return null;
    }
}
