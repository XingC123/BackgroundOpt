package com.venus.backgroundopt.entity;

import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.annotation.UsageComment;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ApplicationInfo;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt;
import com.venus.backgroundopt.manager.process.ProcessManager;
import com.venus.backgroundopt.service.ProcessDaemonService;
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtilsKt;
import com.venus.backgroundopt.utils.log.ILogger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 运行信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class RunningInfo implements ILogger {
    private static RunningInfo runningInfo;

    @NonNull
    public static RunningInfo getInstance() {
        return runningInfo;
    }

    public RunningInfo(ClassLoader classLoader) {
        runningInfo = this;
        this.classLoader = classLoader;
    }

    private final ClassLoader classLoader;

    @NonNull
    public ClassLoader getClassLoader() {
        return classLoader;
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
     * @param userId      用户id
     * @param packageName 包名
     */
    public void removeRecordedNormalApp(int userId, String packageName) {
        String key = getNormalAppKey(userId, packageName);
        NormalAppResult remove = normalApps.remove(key);
        if (remove != null) {
            NormalAppResult.normalAppUidMap.remove(remove.applicationInfo.uid);
        }

        if (BuildConfig.DEBUG) {
            getLogger().debug("移除\"普通app记录\": " + key);
        }
    }

    public void removeAllRecordedNormalApp(String packageName) {
        normalApps.keySet().stream()
                .filter(key -> key.contains(packageName))
                .forEach(key -> {
                    NormalAppResult remove = normalApps.remove(key);
                    if (remove != null) {
                        NormalAppResult.normalAppUidMap.remove(remove.applicationInfo.uid);
                    }
                });
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
        /**
         * (uid, {@link NormalAppResult}) 映射
         * 只有能获取到 {@link ApplicationInfo} 的前提下(即可以获取到uid), 才会进行写入。因此使用 {@link #normalAppUidMap}.get(int)时无需判空
         */
        public static final Map<Integer, NormalAppResult> normalAppUidMap = new HashMap<>();

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
     * 只有非重要系统进程通过key取值才是非null(即, 需要做空检查)
     * uid, AppInfo
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
        return getRunningAppInfo(appInfo.getUid());
    }

    /**
     * 根据uid获取正在运行的列表中的app信息
     *
     * @param uid 要查询的uid(userId+uid)
     * @return 查询到的app信息
     */
    @Nullable
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
     * 设置添加到 {@link #runningApps} 中的 {@link AppInfo}的基本属性
     *
     * @param mProcessRecord app的主进程
     * @param appInfo        要处理的应用信息
     */
    public void setAddedRunningApp(ProcessRecordKt mProcessRecord, AppInfo appInfo) {
        if (mProcessRecord != null) {
            appInfo.addProcess(mProcessRecord);
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().warn(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 的mProcessRecord为空");
            }
        }
    }

    /**
     * 设置添加到 {@link #runningApps} 中的 {@link AppInfo}的基本属性
     *
     * @param appInfo 要处理的应用信息
     */
    public void setAddedRunningApp(AppInfo appInfo) {
        // 找到主进程进行必要设置
        setAddedRunningApp(activityManagerService.findMProcessRecord(appInfo), appInfo);
    }

    /**
     * 若 {@link #runningApps} 中没有此uid记录, 将进行计算创建
     * 注意: 创建后, 会以(uid, {@link AppInfo}[nullable])的形式放入 {@link #runningApps}
     *
     * @param uid 目标app的uid
     * @return 生成的目标app信息(可能为空)
     */
    @UsageComment("仅供OOM调节方法使用")
    @Nullable
    public AppInfo computeRunningAppIfAbsent(int uid) {
        return runningApps.computeIfAbsent(uid, key -> {
            AppInfo[] apps = new AppInfo[]{null};
            /*
                从normalAppResults中查询而不是添加。这个Function只负责当app进入后台并被清理后台之后[自启动]时进行appInfo信息补全。
                对于[开机自启动]的app, 模块暂时不做处理, 一切交由系统。
             */
            Map<Integer, NormalAppResult> normalAppUidMap = NormalAppResult.normalAppUidMap;
            if (normalAppUidMap.containsKey(uid)) {
                NormalAppResult normalAppResult = normalAppUidMap.get(uid);
                ApplicationInfo applicationInfo = normalAppResult.getApplicationInfo();
                if (applicationInfo != null) {
                    String packageName = normalAppResult.getApplicationInfo().getPackageName();
                    ProcessRecordKt mProcessRecord = activityManagerService.findMProcessRecord(packageName, uid);
                    if (mProcessRecord != null) {
                        int userId = mProcessRecord.getUserId();

                        apps[0] = new AppInfo(userId, packageName, this).setUid(uid);
                        setAddedRunningApp(mProcessRecord, apps[0]);
                        apps[0].setAppSwitchEvent(ActivityManagerServiceHook.ACTIVITY_PAUSED);
                        putIntoIdleAppGroup(apps[0]);

                        if (BuildConfig.DEBUG) {
                            getLogger().debug("AppInfo(包名: " + packageName + ", uid: " + uid + ")补充完毕");
                        }
                    }
                }
            }

            return apps[0];
        });
    }

    /**
     * 若 {@link #runningApps} 中没有此uid记录, 将进行计算创建
     *
     * @param userId      目标app对应用户id
     * @param packageName 目标app包名
     * @param uid         目标app的uid
     * @return 生成的目标app信息
     */
    @NonNull
    public AppInfo computeRunningAppIfAbsent(int userId, String packageName, int uid) {
        return runningApps.computeIfAbsent(uid, key -> {
            if (BuildConfig.DEBUG) {
                getLogger().debug("打开新App: " + packageName + ", uid: " + uid);
            }
            AppInfo appInfo = new AppInfo(userId, packageName, this).setUid(uid);
            setAddedRunningApp(appInfo);
            return appInfo;
        });
    }

    /**
     * 根据{@link AppInfo}从运行列表中移除
     *
     * @param appInfo app信息
     */
    public void removeRunningApp(AppInfo appInfo) {
        ConcurrentUtilsKt.lock(appInfo, () -> {
            // 从运行列表移除
            AppInfo remove = runningApps.remove(appInfo.getUid());

            if (remove != null) {
                String packageName = remove.getPackageName();
                // 设置内存分组到死亡分组
                remove.setAppGroupEnum(AppGroupEnum.DEAD);
                // 从待处理列表中移除
                activeAppGroup.remove(appInfo);
                tmpAppGroup.remove(appInfo);
                idleAppGroup.remove(appInfo);

                // app被杀死
                processManager.appDie(remove);

                // 清理AppInfo。也许有助于gc
                appInfo.clearAppInfo();

                if (BuildConfig.DEBUG) {
                    getLogger().debug("移除: " + packageName + ", uid: " + remove.getUid());
                }
            } else {
                if (BuildConfig.DEBUG) {
                    getLogger().warn("移除: 未找到移除项 -> " + appInfo.getPackageName() + ", uid: " + appInfo.getUid());
                }
            }
            return null;
        });
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
        IDLE,
        DEAD
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

//        handlePutInfoActiveAppGroup(appInfo, !switchActivity);

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

    public void putIntoTmpAppGroup(AppInfo appInfo) {
        activeAppGroup.remove(appInfo);
//        idleAppGroup.remove(appInfo); // 没有app在后台也能进入这个方法吧

        /*
            息屏触发 UsageEvents.Event.ACTIVITY_PAUSED 事件。则对当前app按照进入后台处理
            boolean isScreenOn = pm.isInteractive();
            如果isScreenOn值为true，屏幕状态为亮屏或者亮屏未解锁，反之为黑屏。
         */
        if (!getPowerManager().isInteractive()) {
            putIntoIdleAppGroup(appInfo);
        } else {
            tmpAppGroup.add(appInfo);
            appInfo.setAppGroupEnum(AppGroupEnum.TMP);

            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 被放入TmpGroup");
            }
        }
    }

    private void putIntoIdleAppGroup(AppInfo appInfo) {
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

    private void handlePutInfoActiveAppGroup(AppInfo appInfo, boolean needHandleCurApp) {
        // 重置切换事件处理状态
        appInfo.setSwitchEventHandled(false);

        if (needHandleCurApp) {
            handleCurApp(appInfo);

            // 检查前台应用中是否有遗漏的已经没有前台界面的app
            /*
                在Redmi K30p(lmi) MIUI 13 22.7.8 Android12中, 如果先打开应用A, 再从通知栏或弹出的消息打开应用B小窗,
                    再将小窗拉伸至全屏, 此时, 尽管实质上已切换app, 但当前的app切换事件hook仍然捕捉不到A的Activity状态变化。
                    导致无法更新应用A的内存分组。
             */
            activeAppGroup.forEach(app -> {
                if (app.getAppSwitchEvent() == ActivityManagerServiceHook.ACTIVITY_RESUMED &&
                        app.isActivityStopped()) {
                    activeAppGroup.remove(app);
                    putIntoIdleAppGroup(app);
                }
            });
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

        // 启动前台工作
        processManager.appActive(appInfo);

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 被放入ActiveGroup");
        }
    }

    /**
     * 处理上个app
     *
     * @param appInfo app信息
     * @return appInfo是否合法
     */
    public boolean handleLastApp(AppInfo appInfo) {
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

        // 启动后台工作
        processManager.appIdle(appInfo);
//        processManager.setAppToBackgroundProcessGroup(appInfo);

        appInfo.setSwitchEventHandled(true);

        return true;
    }

    /* *************************************************************************
     *                                                                         *
     * 默认桌面                                                                  *
     *                                                                         *
     **************************************************************************/
    private volatile String activeLaunchPackageName = null;

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
        processManager = new ProcessManager(this);
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