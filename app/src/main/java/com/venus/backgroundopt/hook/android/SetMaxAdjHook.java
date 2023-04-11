package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/18
 */
public class SetMaxAdjHook extends MethodHook {
    public SetMaxAdjHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ProcessStateRecord;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.setMaxAdj;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);


            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class};
    }
}
