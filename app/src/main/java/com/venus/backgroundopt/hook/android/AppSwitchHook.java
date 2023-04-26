package com.venus.backgroundopt.hook.android;

import android.app.usage.UsageEvents;
import android.content.ComponentName;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.entity.AppInfo;

import de.robv.android.xposed.XC_MethodHook;

/**
 * app切换的hook
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class AppSwitchHook extends MethodHook {
    /**
     * 进入前台.
     */
    public static final int ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED;
    /**
     * 进入后台.
     */
    public static final int ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED;

    public AppSwitchHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerService;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.updateActivityUsageStats;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                // 获取方法参数
                Object[] args = param.args;

                // 获取切换事件
                int event = (int) args[2];
                if (event != ACTIVITY_PAUSED && event != ACTIVITY_RESUMED) {
                    return;
                }

                // 本次事件用户
                int userId = (int) args[1];
                // 本次事件包名
                String packageName = ((ComponentName) args[0]).getPackageName();
                if (packageName == null) {
                    return;
                }

                RunningInfo runningInfo = getRunningInfo();
                AppInfo appInfo = new AppInfo(userId, packageName);
                if (appInfo.equals(runningInfo.lastAppInfo)) {
                    return;
                }

                runningInfo.lastAppInfo = appInfo;

                debugLog(isDebugMode() &&
                        getLogger().debug(appInfo.getPackageName() + " - isRunning: " + runningInfo.isAppRunning(appInfo)));

                // 该程序第一次运行
                if (!runningInfo.isAppRunning(appInfo)) {
                    if (runningInfo.isNormalApp(appInfo, userId)) { // 该程序不是系统重要进程
                        appInfo.setUid(runningInfo.getNormalAppUid(appInfo, userId));
                        runningInfo.addRunningApp(appInfo);
                    }
                }
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{ClassConstants.ComponentName, int.class, int.class,
                ClassConstants.IBinder, ClassConstants.ComponentName};
    }
}
