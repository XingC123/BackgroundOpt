package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 对安卓虚进程的hook(安卓12引入)
 *
 * @author XingC
 * @version 1.0
 * @date 2023/4/25
 */
public class PhantomProcessHook3 extends MethodHook {
    public PhantomProcessHook3(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerConstants;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.setOverrideMaxCachedProcesses;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                param.args[0] = Integer.MAX_VALUE;
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class};
    }
}
