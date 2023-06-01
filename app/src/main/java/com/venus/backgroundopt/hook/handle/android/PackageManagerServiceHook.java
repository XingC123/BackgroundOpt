package com.venus.backgroundopt.hook.handle.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.AfterHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class PackageManagerServiceHook extends MethodHook {
    public PackageManagerServiceHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.PackageManagerService,
                        MethodConstants.isFirstBoot,
                        new HookAction[]{
                                (AfterHookAction) this::getActiveLaunchPackageName
                        }
                ),
        };
    }

    private Object getActiveLaunchPackageName(XC_MethodHook.MethodHookParam param) {
        RunningInfo runningInfo = getRunningInfo();
        // 在ActivityManagerService加载完毕后再获取
        if (runningInfo.getActivityManagerService() == null) {
            return null;
        }

        // 若已获取默认桌面的包名, 则不进行任何操作
        if (runningInfo.getActiveLaunchPackageName() != null) {
            return null;
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

        return null;
    }
}
