package com.venus.backgroundopt.hook.android;

import android.app.usage.UsageEvents;
import android.content.ComponentName;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.entity.AppInfo;
import com.venus.backgroundopt.server.Process;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

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

    public static final int SIGNAL_10 = 10;

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
                // 检查是否是系统重要进程
                if (!runningInfo.isNormalApp(userId, packageName)) {
                    return;
                }

                // 第一次打开此app
                boolean firstRunning = false;
                AppInfo appInfo = runningInfo.getAppInfoFromRunningApps(userId, packageName);

                if (appInfo == null) {
                    firstRunning = true;

                    appInfo = new AppInfo(userId, packageName);
                    appInfo.setUid(runningInfo.getNormalAppUid(appInfo));
                }

                // 若不是切换app
                if (Objects.equals(appInfo, runningInfo.lastAppInfo)) {
                    return;
                }
                debugLog(isDebugMode() &&
                        getLogger().debug(
                                appInfo.getPackageName() + " 初次运行: " + firstRunning));
                if (firstRunning) {
                    runningInfo.addRunningApp(appInfo);
                } else {
                    handleGC(runningInfo.lastAppInfo);
                }

                runningInfo.lastAppInfo = appInfo;
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{ClassConstants.ComponentName, int.class, int.class,
                ClassConstants.IBinder, ClassConstants.ComponentName};
    }

    /**
     * 处理gc事件
     *
     * @param appInfo app信息
     */
    public void handleGC(AppInfo appInfo) {
        if (Objects.equals(getRunningInfo().getActiveLaunchPackageName(), appInfo.getPackageName())) {
            debugLog(isDebugMode() &&
                    getLogger().debug("当前gc的app为默认桌面, 不进行处理"));
            return;
        }

        // kill -10 pid
        XposedHelpers.callStaticMethod(
                Process.getProcess(classLoader),
                MethodConstants.sendSignal,
                appInfo.getmPid(),
                SIGNAL_10
        );

        debugLog(isDebugMode() &&
                getLogger().debug(appInfo.getPackageName() + " 触发gc, pid = " + appInfo.getmPid()));
    }
}
