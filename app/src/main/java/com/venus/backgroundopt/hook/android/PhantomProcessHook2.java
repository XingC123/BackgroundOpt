package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

/**
 * 对安卓虚进程的hook(安卓12引入)
 *
 * @author XingC
 * @version 1.0
 * @date 2023/4/25
 */
public class PhantomProcessHook2 extends MethodHook {
    public PhantomProcessHook2(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.checkExcessivePowerUsage;
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
        return new Object[]{long.class, boolean.class, long.class, String.class, String.class,
                int.class, ClassConstants.ProcessRecord};
    }
}
