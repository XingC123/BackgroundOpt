package com.venus.backgroundopt.entity;

import android.os.PowerManager;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ApplicationInfo;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;
import com.venus.backgroundopt.manager.process.ProcessManager;
import com.venus.backgroundopt.service.ProcessDaemonService;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 运行信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class RunningInfo implements ILogger {
    private ClassLoader classLoader;

    public RunningInfo() {
    }

    public RunningInfo(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * hook 次数
     */
    private static final Object infoLock = new Object();
    private int hookTimes = Integer.MIN_VALUE;

    public int getHookTimes() {
        synchronized (infoLock) {
            return hookTimes;
        }
    }

    public void updateHookTimes() {
        synchronized (infoLock) {
            if (hookTimes == Integer.MAX_VALUE) {
                hookTimes = Integer.MIN_VALUE;
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 封装的activityManagerService                                             *
     *                                                                         *
     **************************************************************************/
    private ActivityManagerService activityManagerService = null;

    public ActivityManagerService getActivityManagerService() {
        return activityManagerService;
    }

    public void setActivityManagerService(ActivityManagerService activityManagerService) {
        this.activityManagerService = activityManagerService;
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的进程                                                                *
     *                                                                         *
     **************************************************************************/
    public volatile AppInfo lastAppInfo;
    /**
     * 非系统重要进程记录
     * userId包名, ApplicationInfo
     */
    private final Map<String, ApplicationInfo> normalApps = new ConcurrentHashMap<>();
    private final Object checkNormalAppLockObj = new Object();

    /**
     * 是否是普通app
     *
     * @param appInfo app信息
     * @return 是 -> true
     */
    public boolean isNormalApp(AppInfo appInfo) {
        return isNormalApp(appInfo.getUserId(), appInfo.getPackageName()).isNormalApp();
    }

    public NormalAppResult isNormalApp(int userId, String packageName) {
        NormalAppResult normalAppResult = new NormalAppResult();
        String userIdAndPackageName = userId + packageName;

        boolean isNormalApp = normalApps.containsKey(userIdAndPackageName);
        if (isNormalApp) {
            normalAppResult.setNormalApp(true);
            normalAppResult.setApplicationInfo(normalApps.get(userIdAndPackageName));
        } else {
            synchronized (checkNormalAppLockObj) {
                isNormalApp = normalApps.containsKey(userIdAndPackageName);
                if (isNormalApp) {
                    normalAppResult.setNormalApp(true);
                    normalAppResult.setApplicationInfo(normalApps.get(userIdAndPackageName));
                } else {
                    normalAppResult = isImportantSystemApp(packageName);
                    if (normalAppResult.isNormalApp())
                        markNormalApp(userIdAndPackageName, normalAppResult.getApplicationInfo());
                }
            }
        }

        return normalAppResult;
    }

    /**
     * 普通app查找结果
     */
    public static class NormalAppResult {
        private boolean isNormalApp = false;
        private ApplicationInfo applicationInfo;

        public boolean isNormalApp() {
            return isNormalApp;
        }

        public void setNormalApp(boolean normalApp) {
            isNormalApp = normalApp;
        }

        public ApplicationInfo getApplicationInfo() {
            return applicationInfo;
        }

        public void setApplicationInfo(ApplicationInfo applicationInfo) {
            this.applicationInfo = applicationInfo;
        }
    }

    /**
     * 标记为普通app
     *
     * @param userIdAndPackageName userId包名
     * @param applicationInfo      应用信息
     */
    private void markNormalApp(String userIdAndPackageName, ApplicationInfo applicationInfo) {
        normalApps.put(userIdAndPackageName, applicationInfo);
    }

    public int getNormalAppUid(AppInfo appInfo) {
        ApplicationInfo applicationInfo = normalApps.get(appInfo.getPackageName());
        if (applicationInfo == null) {
            return getActivityManagerService().getAppUID(appInfo.getUserId(), appInfo.getPackageName());
        } else {
            return applicationInfo.uid;
        }
    }

    /**
     * 只有非重要系统进程才能放置于此
     * uid, AppInfo
     * 对此map的访问应加锁
     */
    private final Map<Integer, AppInfo> runningApps = new ConcurrentHashMap<>();
    private final Collection<AppInfo> runningAppsInfo = runningApps.values();

    public AppInfo getAppInfoFromRunningApps(int userId, String packageName) {
        AtomicReference<AppInfo> result = new AtomicReference<>();

        runningAppsInfo.parallelStream()
                .filter(appInfo ->
                        Objects.equals(appInfo.getPackageName(), packageName) &&
                                Objects.equals(appInfo.getUserId(), userId))
                .findAny()
                .ifPresent(result::set);

        return result.get();
    }

    public AppInfo getAppInfoFromRunningApps(AppInfo appInfo) {
        return getAppInfoFromRunningApps(appInfo.getUid());
    }

    public AppInfo getAppInfoFromRunningApps(int uid) {
        return getRunningAppInfo(uid);
    }

    /**
     * 根据uid判断是否是系统重要app
     *
     * @param uid uid
     * @return 是 => true
     */
    public boolean isImportantSystemApp(int uid) {
        return runningApps.get(uid) == null;
    }

    /**
     * 根据传入的{@link AppInfo}判断是否是系统重要app
     *
     * @param appInfo app信息
     * @return 是 => true
     */
    public boolean isImportantSystemApp(AppInfo appInfo) {
        return isImportantSystemApp(appInfo.getPackageName()).isNormalApp();
    }

    /**
     * 根据包名判断是否是系统重要app
     *
     * @param packageName 包名
     * @return 是 => true
     */
    public NormalAppResult isImportantSystemApp(String packageName) {
        return getActivityManagerService().isImportantSystemApp(packageName);
    }

    /**
     * 根据uid获取正在运行的列表中的app信息
     *
     * @param uid 要查询的uid
     * @return 查询到的app信息
     */
    public AppInfo getRunningAppInfo(int uid) {
        return runningApps.get(uid);
    }

    /**
     * 根据传入的{@link AppInfo}判断是否在运行列表
     *
     * @param appInfo app信息
     * @return 运行 => true
     */
    public boolean isAppRunning(AppInfo appInfo) {
        return runningAppsInfo.contains(appInfo);
    }

    /**
     * 添加app信息到运行列表
     *
     * @param appInfo app信息
     */
    public void addRunningApp(AppInfo appInfo) {
        // 找到主进程进行必要设置
        ProcessRecord mProcessRecord = findMProcessRecord(appInfo);

        if (mProcessRecord != null) {
            // 设置主进程的最大adj(保活)
            mProcessRecord.setDefaultMaxAdj();
            // 保存进程信息
            appInfo.setmProcessInfo(mProcessRecord);
            // 保存主进程
            appInfo.setmProcessRecord(mProcessRecord);
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().warn(appInfo.getPackageName() + " 的mProcessRecord为空");
            }
        }

        // 添加到运行列表
        runningApps.put(appInfo.getFixedUid(), appInfo);
    }

    /**
     * 根据{@link AppInfo}找到其主进程
     *
     * @param appInfo app信息
     * @return 主进程的记录
     */
    public ProcessRecord findMProcessRecord(AppInfo appInfo) {
        return activityManagerService.findMProcessRecord(appInfo);
    }

    public ProcessRecord getTargetProcessRecord(int pid) {
        return getActivityManagerService().getProcessList().getTargetProcessRecord(pid);
    }

    /**
     * 根据{@link AppInfo}从运行列表中移除
     *
     * @param appInfo app信息
     */
    public void removeRunningApp(AppInfo appInfo) {
        if (Objects.equals(appInfo, lastAppInfo)) {
            lastAppInfo = null;
        }

        // 从运行列表移除
        AppInfo remove = runningApps.remove(appInfo.getFixedUid());

        if (BuildConfig.DEBUG) {
            getLogger().debug("移除: " + (remove == null ? "未找到包名" : remove.getPackageName()));
        }

        // 从待处理列表中移除
        activeAppGroup.remove(appInfo);
        tmpAppGroup.remove(appInfo);
        idleAppGroup.remove(appInfo);
        handleRemoveFromActiveAppGroup(appInfo);
        handleRemoveFromIdleAppGroup(appInfo);

        // 清理AppInfo。也许有助于gc
        appInfo.clearAppInfo();
    }

    /* *************************************************************************
     *                                                                         *
     * app切换待处理队列                                                          *
     *                                                                         *
     **************************************************************************/
    // 活跃分组
    private final Set<AppInfo> activeAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 缓存分组
    private final Set<AppInfo> tmpAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 后台分组
    private final Set<AppInfo> idleAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void putIntoActiveAppGroup(AppInfo appInfo, boolean firstRunning) {
        boolean switchActivity = false;  // 是否是应用内activity的切换
        /*
            处理当前元素
         */
        if (firstRunning) { // 第一次运行直接添加
            handlePutInfoActiveAppGroup(appInfo, true);
        } else {
            if (tmpAppGroup.remove(appInfo)) {  // app: Activity切换
                handlePutInfoActiveAppGroup(appInfo, false);
                switchActivity = true;
            } else if (idleAppGroup.remove(appInfo)) {  // 从后台组移除
                handleRemoveFromIdleAppGroup(appInfo);
                handlePutInfoActiveAppGroup(appInfo, true);   // 此行可以抽取。但为了保持逻辑清晰, 依然放在此处
            }
        }

        /*
            处理其他分组
         */
        if (!switchActivity) {
            // 检查缓存分组
            tmpAppGroup.parallelStream().forEach(app -> {
                if (app.getAppSwitchEvent() == ActivityManagerServiceHook.ACTIVITY_PAUSED) {
                    tmpAppGroup.remove(app);
                    putIntoIdleAppGroup(app);
                } else {
                    tmpAppGroup.remove(app);
                    handlePutInfoActiveAppGroup(appInfo, false);
                }
            });

            // 检查后台分组(宗旨是在切换后台时执行)
            idleAppGroup.parallelStream().forEach(app -> {
                if (app.getAppSwitchEvent() == ActivityManagerServiceHook.ACTIVITY_RESUMED) {
                    // 从后台分组移除
                    idleAppGroup.remove(app);
                    handleRemoveFromIdleAppGroup(app);

                    handlePutInfoActiveAppGroup(appInfo, true);
                }
            });
        }

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + " 的active事件处理完毕");
        }
    }

    private void handlePutInfoActiveAppGroup(AppInfo appInfo, boolean needHandleCurApp) {
        // 重置切换事件处理状态
        appInfo.setSwitchEventHandled(false);

        if (needHandleCurApp) {
            handleCurApp(appInfo);
        }

        activeAppGroup.add(appInfo);
    }

    private void handleCurApp(AppInfo appInfo) {
        if (appInfo == null) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("前台app已被杀死, 不执行处理");
            }

            return;
        }

        if (Objects.equals(getActiveLaunchPackageName(), appInfo.getPackageName())) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("前台app为默认桌面, 不进行处理");
            }
            return;
        }

        // 启动MemoryTrimTask任务
        processManager.startForegroundAppTrimTask(appInfo.getmProcessRecord());

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + " 被放入ActiveGroup");
        }
    }

    public void putIntoTmpAppGroup(AppInfo appInfo) {
        tmpAppGroup.add(appInfo);
        activeAppGroup.remove(appInfo);
//        idleAppGroup.remove(appInfo); // 没有app在后台也能进入这个方法吧

        /*
            息屏触发 UsageEvents.Event.ACTIVITY_PAUSED 事件。则对当前app按照进入后台处理
            boolean isScreenOn = pm.isInteractive();
            如果isScreenOn值为true，屏幕状态为亮屏或者亮屏未解锁，反之为黑屏。
         */
        if (!getPowerManager().isInteractive()) {
            putIntoIdleAppGroup(appInfo);
        }

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + " 被放入TmpGroup");
        }
    }

    private void putIntoIdleAppGroup(AppInfo appInfo) {
        handleRemoveFromActiveAppGroup(appInfo);
        // 做app清理工作
        handleLastApp(appInfo);

        idleAppGroup.add(appInfo);

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + "  被放入IdleGroup");
        }
    }

    private void handleRemoveFromActiveAppGroup(AppInfo appInfo) {
        // 移除某些定时
        processManager.removeForegroundAppTrimTask(appInfo.getmProcessRecord());
    }

    private void handleRemoveFromIdleAppGroup(AppInfo appInfo) {
        // 移除某些定时
        processManager.removeBackgroundAppTrimTask(appInfo.getmProcessRecord());
    }

    private void handleLastApp(AppInfo appInfo) {
        if (appInfo == null) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("待执行app已被杀死, 不执行处理");
            }

            return;
        }

        if (Objects.equals(getActiveLaunchPackageName(), appInfo.getPackageName())) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("当前操作的app为默认桌面, 不进行处理");
            }
            return;
        }

        if (appInfo.isSwitchEventHandled()) {
            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + " 的切换事件已经处理过");
            }
            return;
        }

        processManager.startBackgroundAppTrimTask(appInfo.getmProcessRecord());
        processManager.handleGC(appInfo);
//        compactApp(appInfo);

        appInfo.setSwitchEventHandled(true);
    }

    /* *************************************************************************
     *                                                                         *
     * 默认桌面                                                                  *
     *                                                                         *
     **************************************************************************/
    private String activeLaunchPackageName = null;

    public String getActiveLaunchPackageName() {
        return activeLaunchPackageName;
    }

    public void setActiveLaunchPackageName(String activeLaunchPackageName) {
        this.activeLaunchPackageName = activeLaunchPackageName;
    }

    /* *************************************************************************
     *                                                                         *
     * process_daemon_service                                                  *
     *                                                                         *
     **************************************************************************/ ProcessDaemonService processDaemonService;

    public ProcessDaemonService getProcessDaemonService() {
        return processDaemonService;
    }

    public void initProcessDaemonService() {
        try {
            this.processDaemonService = new ProcessDaemonService();
            this.processDaemonService.initPds();
        } catch (IOException e) {
            getLogger().error("process_daemon_service加载失败", e);
        }
    }

    /* *************************************************************************
     *                                                                         *
     * process_manager                                                         *
     *                                                                         *
     **************************************************************************/
    private ProcessManager processManager;

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public void initProcessManager() {
        processManager = new ProcessManager(this.activityManagerService);
    }

    /* *************************************************************************
     *                                                                         *
     * 电源管理                                                                  *
     *                                                                         *
     **************************************************************************/
    private PowerManager powerManager;

    public PowerManager getPowerManager() {
        if (powerManager == null) {
            powerManager = activityManagerService.getContext().getSystemService(PowerManager.class);
        }

        return powerManager;
    }
}
