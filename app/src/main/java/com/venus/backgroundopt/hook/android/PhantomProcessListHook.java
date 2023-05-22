package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/25
 */
public class PhantomProcessListHook extends MethodHook {
    public PhantomProcessListHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.PhantomProcessList;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.trimPhantomProcessesIfNecessary;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                return null;
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }
}
