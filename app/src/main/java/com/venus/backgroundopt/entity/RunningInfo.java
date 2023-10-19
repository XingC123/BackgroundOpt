package com.venus.backgroundopt.entity;

import android.content.ComponentName;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.annotation.UsageComment;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt;
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
import java.util.concurrent.locks.Lock;
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
                        handleActivityEventChange(
                                ActivityManagerServiceHookKt.ACTIVITY_STOPPED,
                                null,
                                apps[0]
                        );

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

    // 后台分组
    private final Set<AppInfo> idleAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 处理Activity改变事件
     * 本方法处理的共有6大种情况, 本质就是从中找出切换app的方法:
     * <pre>
     *     a为第一个打开的Activity, b为第二个。
     *     1. 初次打开: event(1[a])
     *     2. 从后台到前台: event(23[a]) -> event(1[a])
     *     3. 同一app内部:
     *          1) Activity(a) -> Activity(b):  event(2[a]) -> event(1[b]) -> event(23[a])
     *          1) Activity(b) -> Activity(a):  event(2[b]) -> event(1[a]) -> event(24[b])
     *     4. 退至后台: event(2[a]) -> event(23[a])
     *     5. 其他app小窗到全屏: event(1[a]) -> event(23[a])
     *     6. App关闭?: event(x[a]) -> event(24[a])
     * </pre>
     *
     * @param event         事件码
     * @param componentName 当前组件
     * @param appInfo       app
     */
    public void handleActivityEventChange(int event, ComponentName componentName, @NonNull AppInfo appInfo) {
        Lock appInfoLock = appInfo.getLock();
        appInfoLock.lock();
        switch (event) {
            case ActivityManagerServiceHookKt.ACTIVITY_RESUMED -> {
                // 从后台到前台 || 第一次打开app
                if (Objects.equals(componentName, appInfo.getComponentName())
                        && appInfo.getAppSwitchEvent() == ActivityManagerServiceHookKt.ACTIVITY_STOPPED
                        || appInfo.getComponentName() == null
                ) {
                    putIntoActiveAppGroup(appInfo);
                }
            }

            /*case ActivityManagerServiceHookKt.ACTIVITY_PAUSED -> {
                // do nothing
            }*/

            case ActivityManagerServiceHookKt.ACTIVITY_STOPPED -> {
                /*
                    23.10.18: appInfo.getAppGroupEnum() != AppGroupEnum.IDLE可以换成appInfo.getAppGroupEnum() == AppGroupEnum.ACTIVE,
                        但为了往后的兼容性, 暂时保持这样
                 */
                if (Objects.equals(componentName, appInfo.getComponentName())
                        && (appInfo.getAppGroupEnum() != AppGroupEnum.IDLE || !getPowerManager().isInteractive())
                ) {
                    putIntoIdleAppGroup(appInfo);
                }
            }

            default -> {
//                if (event == ActivityManagerServiceHookKt.ACTIVITY_DESTROYED) {
//                    // do nothing
//                }
            }
        }

        // 更新app的切换状态
        appInfo.setAppSwitchEvent(event);
        appInfo.setComponentName(componentName);

        appInfoLock.unlock();
    }

    private void putIntoActiveAppGroup(@NonNull AppInfo appInfo) {
        // 重置切换事件处理状态
        appInfo.setSwitchEventHandled(false);

        // 处理当前app
        handleCurApp(appInfo);

        activeAppGroup.add(appInfo);
        appInfo.setAppGroupEnum(AppGroupEnum.ACTIVE);

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 被放入ActiveGroup");
        }
    }

    private void putIntoIdleAppGroup(@NonNull AppInfo appInfo) {
        // 处理上个app
        handleLastApp(appInfo);

        idleAppGroup.add(appInfo);
        appInfo.setAppGroupEnum(AppGroupEnum.IDLE);

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + "  被放入IdleGroup");
        }
    }

    private void handleCurApp(@NonNull AppInfo appInfo) {
        if (Objects.equals(getActiveLaunchPackageName(), appInfo.getPackageName())) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("前台app为默认桌面, 不进行处理");
            }
            return;
        }

        // 启动前台工作
        processManager.appActive(appInfo);
    }

    /**
     * 处理上个app
     *
     * @param appInfo app信息
     */
    private void handleLastApp(@NonNull AppInfo appInfo) {
        if (Objects.equals(getActiveLaunchPackageName(), appInfo.getPackageName())) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("当前操作的app为默认桌面, 不进行处理");
            }
            return;
        }

        if (appInfo.isSwitchEventHandled()) {
            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 的切换事件已经处理过");
            }
            return;
        }

        // 启动后台工作
        processManager.appIdle(appInfo);
//        processManager.setAppToBackgroundProcessGroup(appInfo);

        appInfo.setSwitchEventHandled(true);
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