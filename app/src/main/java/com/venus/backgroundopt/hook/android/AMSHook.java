package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.server.ActivityManagerService;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 对ActivityManagerService的hook
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class AMSHook extends MethodHook {

    public AMSHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.setSystemProcess;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                ActivityManagerService ams = new ActivityManagerService(param.thisObject);
                getRunningInfo().setActivityManagerService(ams);
                debugLog(isDebugMode() && getLogger().debug("拿到AMS"));

                // 设置persist.sys.spc.enabled禁用小米的杀后台
                XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass(ClassConstants.SystemProperties, classLoader),
                        MethodConstants.set,
                        "persist.sys.spc.enabled", "false");
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }
}
