package com.venus.backgroundopt.hook.miui;

import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

/**
 * 小米睡眠模式防杀后台
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/17
 */
public class ClearAppHook extends MethodHook {
    public ClearAppHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.SleepModeControllerNew;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.clearApp;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return null;
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }
}
