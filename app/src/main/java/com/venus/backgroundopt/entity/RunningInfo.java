package com.venus.backgroundopt.entity;

import android.os.PowerManager;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ApplicationInfo;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.manager.process.ProcessManager;
import com.venus.backgroundopt.service.ProcessDaemonService;
import com.venus.backgroundopt.utils.log.ILogger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 运行信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class RunningInfo implements ILogger {
    public RunningInfo() {
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
     * 非系统重要进程映射表                                                        *
     *                                                                         *
     **************************************************************************/
    /**
     * 非系统重要进程记录
     * key: 主用户 -> packageName, 其他用户: userId:packageName
     * value: NormalAppResult
     * 例:
     * <pre>
     *     1) (userId = 0, packageName=com.venus.aaa) -> (key): com.venus.aaa
     *     2) (userId = 999, packageName=com.venus.aaa) -> (key): com.venus.aaa:999
     * </pre>
     */
    private final Map<String, NormalAppResult> normalApps = new ConcurrentHashMap<>();

    public List<Integer> getNormalUIDs() {
        return normalApps.values().stream()
                .map(normalAppResult -> normalAppResult.getApplicationInfo().uid)
                .collect(Collectors.toList());
    }

    public Collection<NormalAppResult> getNormalAppResults() {
        return normalApps.values();
    }

    /**
     * 获取放进{@link #normalApps}的key
     *
     * @param userId      用户id
     * @param packageName 包名
     * @return key
     */
    public String getNormalAppKey(int userId, String packageName) {
        if (userId == ActivityManagerService.MAIN_USER) {
            return packageName;
        }
        return userId + ":" + packageName;
    }

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
        return normalApps.computeIfAbsent(getNormalAppKey(userId, packageName), key -> isImportantSystemApp(userId, packageName));
    }

    /**
     * 移除给定key匹配的{@link NormalAppResult}
     *
     * @param key 见{@link #getNormalAppKey}
     */
    public void removeRecordedNormalApp(String key) {
        normalApps.remove(key);
        if (BuildConfig.DEBUG) {
            getLogger().debug("移除\"普通app记录\": " + key);
        }
    }

    public void removeAllRecordedNormalApp(String packageName) {
        normalApps.keySet().stream()
                .filter(key -> key.contains(packageName))
                .forEach(normalApps::remove);
        if (BuildConfig.DEBUG) {
            getLogger().debug("移除\"普通app记录\": " + packageName);
        }
    }

    /**
     * 根据包名判断是否是系统重要app
     *
     * @param packageName 包名
     * @return 是 => true
     */
    public NormalAppResult isImportantSystemApp(int userId, String packageName) {
        return getActivityManagerService().isImportantSystemApp(userId, packageName);
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

        public NormalAppResult setNormalApp(boolean normalApp) {
            isNormalApp = normalApp;

            return this;
        }

        public ApplicationInfo getApplicationInfo() {
            return applicationInfo;
        }

        public void setApplicationInfo(ApplicationInfo applicationInfo) {
            this.applicationInfo = applicationInfo;
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的进程                                                                *
     *                                                                         *
     **************************************************************************/
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

    public AppInfo getAppInfoFromRunningApps(int repairedUid) {
        return getRunningAppInfo(repairedUid);
    }

    /**
     * 根据uid获取正在运行的列表中的app信息
     *
     * @param uid 要查询的uid(userId+uid)
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
        setAddedRunningApp(appInfo);
        // 添加到运行列表
        runningApps.put(appInfo.getUid(), appInfo);
    }

    public void setAddedRunningApp(ProcessRecord mProcessRecord, AppInfo appInfo) {
        if (mProcessRecord != null) {
            // 设置主进程的最大adj(保活)
            mProcessRecord.setDefaultMaxAdj();
            appInfo.setMProcessInfoAndMProcessRecord(mProcessRecord);
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().warn(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 的mProcessRecord为空");
            }
        }
    }

    public void setAddedRunningApp(AppInfo appInfo) {
        // 找到主进程进行必要设置
        ProcessRecord mProcessRecord = findMProcessRecord(appInfo);
        setAddedRunningApp(mProcessRecord, appInfo);
    }

    public AppInfo computeRunningAppIfAbsent(int uid, Function<Integer, AppInfo> function) {
        return runningApps.computeIfAbsent(uid, function);
    }

    public AppInfo computeRunningAppIfAbsent(int userId, String packageName, int uid) {
        return runningApps.computeIfAbsent(uid, key -> {
            if (BuildConfig.DEBUG) {
                getLogger().debug("创建新进程: " + packageName + ", uid: " + uid);
            }
            AppInfo appInfo = new AppInfo(userId, packageName, this).setUid(uid);
            setAddedRunningApp(appInfo);
            return appInfo;
        });
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
        // 从运行列表移除
        AppInfo remove = runningApps.remove(appInfo.getUid());

        if (remove != null) {
            // 从待处理列表中移除
            activeAppGroup.remove(appInfo);
            tmpAppGroup.remove(appInfo);
            idleAppGroup.remove(appInfo);
            processManager.removeAllAppMemoryTrimTask(appInfo);

            // 清理待压缩进程
            processManager.cancelCompactProcessInfo(appInfo);

            // 清理AppInfo。也许有助于gc
            appInfo.clearAppInfo();

            if (BuildConfig.DEBUG) {
                getLogger().debug("移除: " + remove.getPackageName() + ", uid: " + remove.getUid());
            }
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().warn("移除: 未找到移除项 -> " + appInfo.getPackageName() + ", uid: " + appInfo.getUid());
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * app切换待处理队列                                                          *
     *                                                                         *
     **************************************************************************/
    public enum AppGroupEnum {
        NONE,
        ACTIVE,
        TMP,
        IDLE
    }

    // 活跃分组
    private final Set<AppInfo> activeAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 缓存分组
    private final Set<AppInfo> tmpAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 后台分组
    private final Set<AppInfo> idleAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void putIntoActiveAppGroup(AppInfo appInfo) {
        boolean switchActivity = false;  // 是否是应用内activity的切换
        /*
            处理当前元素
         */
        if (Objects.equals(AppGroupEnum.NONE, appInfo.getAppGroupEnum())) { // 第一次运行直接添加
            handlePutInfoActiveAppGroup(appInfo, true);
        } else if (tmpAppGroup.remove(appInfo)) {  // app: Activity切换
            handlePutInfoActiveAppGroup(appInfo, false);
            switchActivity = true;
        } else if (idleAppGroup.remove(appInfo)) {  // 从后台组移除
            handlePutInfoActiveAppGroup(appInfo, true);
        }

        /*
            处理其他分组
         */
        if (!switchActivity) {  // 意味着非应用内切换Activity, 即某应用前后台状态被改变
            // 检查缓存分组, 以确定是否需要改变前后台状态
            /*
                理论来说, 只有app.getAppSwitchEvent()== ActivityManagerServiceHook.ACTIVITY_PAUSED才能
                被放置于tmpGroup。但由于响应的滞后性, 可能在期间发生了app状态切换, 因此仍然检查是否是
                ActivityManagerServiceHook.ACTIVITY_PAUSED来做不同操作。
                这里也许是可优化的点。
             */
            tmpAppGroup.forEach(app -> {
                if (app.getAppSwitchEvent() == ActivityManagerServiceHook.ACTIVITY_PAUSED) {
                    tmpAppGroup.remove(app);
                    putIntoIdleAppGroup(app);
                } else {
                    tmpAppGroup.remove(app);
                    handlePutInfoActiveAppGroup(appInfo, false);
                }
            });
        }

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 的active事件处理完毕");
        }
    }

    private void handlePutInfoActiveAppGroup(AppInfo appInfo, boolean needHandleCurApp) {
        // 重置切换事件处理状态
        appInfo.setSwitchEventHandled(false);

        if (needHandleCurApp) {
            handleCurApp(appInfo);
        }

        activeAppGroup.add(appInfo);
        appInfo.setAppGroupEnum(AppGroupEnum.ACTIVE);
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
            getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 被放入ActiveGroup");
        }
    }

    public void putIntoTmpAppGroup(AppInfo appInfo) {
        tmpAppGroup.add(appInfo);
        activeAppGroup.remove(appInfo);
//        idleAppGroup.remove(appInfo); // 没有app在后台也能进入这个方法吧

        appInfo.setAppGroupEnum(AppGroupEnum.TMP);

        /*
            息屏触发 UsageEvents.Event.ACTIVITY_PAUSED 事件。则对当前app按照进入后台处理
            boolean isScreenOn = pm.isInteractive();
            如果isScreenOn值为true，屏幕状态为亮屏或者亮屏未解锁，反之为黑屏。
         */
        if (!getPowerManager().isInteractive()) {
            putIntoIdleAppGroup(appInfo);
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 被放入TmpGroup");
            }
        }
    }

    public void putIntoIdleAppGroup(AppInfo appInfo) {
        // 做app清理工作
        boolean valid = handleLastApp(appInfo);
        if (valid) {
            idleAppGroup.add(appInfo);
            appInfo.setAppGroupEnum(AppGroupEnum.IDLE);

            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + "  被放入IdleGroup");
            }
        }
    }

    /**
     * 处理上个app
     *
     * @param appInfo app信息
     * @return appInfo是否合法
     */
    private boolean handleLastApp(AppInfo appInfo) {
        if (appInfo == null) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("待执行app已被杀死, 不执行处理");
            }

            return false;
        }

        if (Objects.equals(getActiveLaunchPackageName(), appInfo.getPackageName())) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("当前操作的app为默认桌面, 不进行处理");
            }
            return true;
        }

        if (appInfo.isSwitchEventHandled()) {
            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 的切换事件已经处理过");
            }
            return true;
        }

        processManager.startBackgroundAppTrimTask(appInfo.getmProcessRecord());
//        processManager.handleGC(appInfo);
//        processManager.setAppToBackgroundProcessGroup(appInfo);

        appInfo.setSwitchEventHandled(true);

        return true;
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
     **************************************************************************/
    ProcessDaemonService processDaemonService;

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