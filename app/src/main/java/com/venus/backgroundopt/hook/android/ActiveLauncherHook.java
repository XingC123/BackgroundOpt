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
        return ClassConstants.PackageManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.isFirstBoot;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                RunningInfo runningInfo = getRunningInfo();
                // 在ActivityManagerService加载完毕后再获取
                if (runningInfo.getActivityManagerService() == null) {
                    return;
                }

                if (runningInfo.getActiveLaunchPackageName() != null) {
                    return;
                }

                Object mPackageManagerService = param.thisObject;
                Object mInjector =
                        XposedHelpers.getObjectField(mPackageManagerService, FieldConstants.mInjector);
                Object mDefaultAppProvider =
                        XposedHelpers.callMethod(mInjector, MethodConstants.getDefaultAppProvider);

                String packageName = (String) XposedHelpers.callMethod(
                        mDefaultAppProvider,
                        MethodConstants.getDefaultHome,
                        0);

                debugLog(isDebugMode() &&
                        getLogger().debug("默认启动器为: " + packageName));

                runningInfo.setActiveLaunchPackageName(packageName);
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }
}
