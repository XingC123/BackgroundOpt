package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/5/30
 */
public class ActiveLauncherHook extends MethodHook {
    public ActiveLauncherHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.SystemServer;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.startBootstrapServices;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Object SystemServer = param.thisObject;
                Object mPackageManagerService = XposedHelpers.getObjectField(SystemServer, FieldConstants.mPackageManagerService);

//                Object defaultAppProvider = XposedHelpers.callMethod(
//                        mPackageManagerService,
//                        MethodConstants.getDefaultAppProvider);
                Object mDefaultAppProvider =
                        XposedHelpers.getObjectField(mPackageManagerService, FieldConstants.mDefaultAppProvider);

                String packageName = (String) XposedHelpers.callMethod(
                        mDefaultAppProvider,
                        MethodConstants.getDefaultHome,
                        0);

                debugLog(isDebugMode() &&
                        getLogger().debug("默认启动器为: " + packageName));

                getRunningInfo().setActiveLaunchPackageName(packageName);
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{ClassConstants.TimingsTraceAndSlog};
    }
}
