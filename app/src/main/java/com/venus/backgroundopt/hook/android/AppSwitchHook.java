package com.venus.backgroundopt.hook.android;

import android.app.usage.UsageEvents;
import android.content.ComponentName;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.entity.AppInfo;
import com.venus.backgroundopt.server.ActivityManagerService;
import com.venus.backgroundopt.server.CachedAppOptimizer;
import com.venus.backgroundopt.server.Process;
import com.venus.backgroundopt.server.ProcessRecord;

import java.util.List;
import java.util.Objects;

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

    /**
     * {@link android.os.Process#SIGNAL_USR1}
     */
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
                    handleLastApp(runningInfo.lastAppInfo);
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

    private void handleLastApp(AppInfo appInfo) {
        if (Objects.equals(getRunningInfo().getActiveLaunchPackageName(), appInfo.getPackageName())) {
            debugLog(isDebugMode() &&
                    getLogger().debug("当前处理的app为默认桌面, 不进行处理"));
            return;
        }

        handleGC(appInfo);
        compactApp(appInfo);
    }

    /**
     * 处理gc事件
     *
     * @param appInfo app信息
     */
    private void handleGC(AppInfo appInfo) {
        // kill -10 pid
        Process.sendSignal(appInfo.getmPid(), SIGNAL_10);

        debugLog(isDebugMode() &&
                getLogger().debug(appInfo.getPackageName() + " 触发gc, pid = " + appInfo.getmPid()));
    }

    /**
     * 压缩app
     *
     * @param appInfo app信息
     */
    private void compactApp(AppInfo appInfo) {
        ActivityManagerService activityManagerService = getRunningInfo().getActivityManagerService();

        // 获取进程列表
        List<ProcessRecord> processRecords = activityManagerService.getProcessList().getProcessRecords(appInfo);

        if (processRecords.size() == 0) {
            debugLog(isDebugMode() &&
                    getLogger().warn(appInfo.getPackageName() + ": 未找到进程, 不执行压缩"));
            return;
        }

        // 执行压缩
        processRecords.forEach(processRecord -> {
                    CachedAppOptimizer cachedAppOptimizer = activityManagerService.getOomAdjuster().getCachedAppOptimizer();
                    cachedAppOptimizer.compactProcess(processRecord.getPid(), CachedAppOptimizer.COMPACT_ACTION_ANON);
                }
        );

        debugLog(isDebugMode() &&
                getLogger().debug(appInfo.getPackageName() + ": 压缩流程执行完毕"));
    }
}
