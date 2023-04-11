package com.venus.backgroundopt.hook.android;

import android.os.Build;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class OomAdjusterHook extends MethodHook {
    public OomAdjusterHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.OomAdjuster;
    }

    @Override
    public String getTargetMethod() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return MethodConstants.setCurAdj;
        } else {    // R/Q
            return MethodConstants.applyOomAdjLocked;
        }
//        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R || Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
//            XposedHelpers.findAndHookMethod(ClassEnum.OomAdjuster, classLoader, MethodEnum.applyOomAdjLocked, ClassEnum.ProcessRecord, boolean.class, long.class, long.class, new OomAdjHook(classLoader, memData, OomAdjHook.Android_Q_R));
//
//        }
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
        return new Object[0];
    }
}
