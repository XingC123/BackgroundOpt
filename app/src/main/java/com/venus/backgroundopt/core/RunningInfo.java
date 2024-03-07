package com.venus.backgroundopt.core;

import android.content.ComponentName;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.annotation.UsageComment;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.FindAppResult;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.MemInfoReader;
import com.venus.backgroundopt.hook.handle.android.entity.PackageManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt;
import com.venus.backgroundopt.manager.process.ProcessManager;
import com.venus.backgroundopt.service.ProcessDaemonService;
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtils;
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtilsKt;
import com.venus.backgroundopt.utils.log.ILogger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import de.robv.android.xposed.XC_MethodHook;

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

        loadMemInfoReader();
    }

    private final ClassLoader classLoader;

    @NonNull
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private MemInfoReader memInfoReader;

    public MemInfoReader getMemInfoReader() {
        return memInfoReader;
    }

    private void loadMemInfoReader() {
        memInfoReader = MemInfoReader.getInstance(classLoader);
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
     * ApplicationInfo的结果集                                                   *
     *                                                                         *
     **************************************************************************/
    /**
     * 每次打开app, 都会获取{@link android.content.pm.ApplicationInfo}。本集合用来缓存结果。<br>
     * key: 主用户 -> packageName, 其他用户: userId:packageName<br>
     * value: {@link FindAppResult}<br>
     * 例:
     * <pre>
     *     1) (userId = 0, packageName=com.venus.aaa) -> (key): com.venus.aaa
     *     2) (userId = 999, packageName=com.venus.aaa) -> (key): com.venus.aaa:999
     * </pre>
     */
    public final Map<String, FindAppResult> findAppResultMap = new ConcurrentHashMap<>();

    public boolean isImportantSystemApp(int userId, String packageName) {
        try {
            return getFindAppResult(userId, packageName).getImportantSystemApp();
        } catch (Throwable throwable) {
            getLogger().error("判断是否是重要app出错: userId: " + userId + ", packageName: " + packageName, throwable);
            return false;
        }
    }

    @NonNull
    public FindAppResult getFindAppResult(int userId, String packageName) {
        return findAppResultMap.computeIfAbsent(
                getNormalAppKey(userId, packageName),
                key -> activityManagerService.getFindAppResult(userId, packageName)
        );
    }

    /**
     * 获取放进{@link #findAppResultMap}的key
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
     * 移除给定key匹配的{@link FindAppResult}
     *
     * @param userId      用户id
     * @param packageName 包名
     */
    public void removeRecordedFindAppResult(int userId, String packageName) {
        String key = getNormalAppKey(userId, packageName);
        findAppResultMap.remove(key);

        if (BuildConfig.DEBUG) {
            getLogger().debug("移除匹配的app记录: userId: " + userId + ", 包名: " + packageName);
        }
    }

    public void removeAllRecordedFindAppResult(String packageName) {
        findAppResultMap.keySet().stream()
                .filter(key -> key.contains(packageName))
                .forEach(findAppResultMap::remove);
        if (BuildConfig.DEBUG) {
            getLogger().debug("移除所有匹配的app记录: 包名: " + packageName);
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的应用                                                                *
     *                                                                         *
     **************************************************************************/
    /**
     * 运行中的app
     * <userId+packageName, AppInfo>
     */
    private final Map<String, AppInfo> runningApps = new ConcurrentHashMap<>();
    private final Collection<AppInfo> runningAppsInfo = runningApps.values();

    @Deprecated
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

    public Collection<AppInfo> getRunningAppInfos() {
        return runningAppsInfo;
    }

    /**
     * 获取运行中的app的标识符
     *
     * @param userId      目标{@link AppInfo}所属的用户id
     * @param packageName 目标{@link AppInfo}所属的包名
     * @return 从 {@link #runningApps}中获取{@link AppInfo}所需的标识符
     */
    @NonNull
    public String getRunningAppIdentifier(int userId, String packageName) {
        return getNormalAppKey(userId, packageName);
    }

    /**
     * 根据uid获取正在运行的列表中的app信息<br>
     * 计算运行中app的map的key需要userId + packageName。<br>
     * 请使用: getRunningAppInfo(userId,packageName)
     *
     * @param uid 要查询的uid(userId+uid)
     * @return 查询到的app信息
     */
    // 在2024.2.18的commit, 即"ApplicationInfo的结果集"初次进版时, runningApps的value不会再有null
    // 但为了保证后续兼容性, 仍然注解为Nullable
    // @NonNull
    @Deprecated
    @Nullable
    public AppInfo getRunningAppInfo(int uid) {
        return runningApps.get(uid);
    }

    @Nullable
    public AppInfo getRunningAppInfo(int userId, String packageName) {
        return runningApps.get(getRunningAppIdentifier(userId, packageName));
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
        return runningApps.computeIfAbsent(getRunningAppIdentifier(userId, packageName), key -> {
            if (BuildConfig.DEBUG) {
                getLogger().debug("创建新App记录: " + packageName + ", uid: " + uid);
            }
            AppInfo appInfo = new AppInfo(userId, packageName, this).setUid(uid);
            appInfo.setFindAppResult(getFindAppResult(userId, packageName));
            return appInfo;
        });
    }

    /**
     * 根据{@link AppInfo}从运行列表中移除
     *
     * @param appInfo app信息
     */
    public void removeRunningApp(@NonNull AppInfo appInfo) {
        String packageName = appInfo.getPackageName();
        if (packageName == null) {
            getLogger().warn("kill: 包名为空");
            return;
        }
        ConcurrentUtilsKt.lock(appInfo, () -> {
            // 从运行列表移除
            AppInfo remove = runningApps.remove(getRunningAppIdentifier(appInfo.getUserId(), packageName));

            if (remove != null) {
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
                    getLogger().debug("kill: userId: " + appInfo.getUserId() + ", packageName: " + packageName + " >>> 杀死App");
                }
            } else {
                if (BuildConfig.DEBUG) {
                    getLogger().warn("kill: 未找到移除项 -> userId: " + appInfo.getUserId() + ", packageName: " + packageName);
                }
            }
            return null;
        });
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的进程                                                                *
     *                                                                         *
     **************************************************************************/
    private final Map<Integer, ProcessRecordKt> runningProcesses = new ConcurrentHashMap<>();

    @Nullable
    public ProcessRecordKt getRunningProcess(int pid) {
        return runningProcesses.get(pid);
    }

    @NonNull
    public Collection<ProcessRecordKt> getRunningProcesses() {
        return runningProcesses.values();
    }

    private void putIntoRunningProcesses(int pid, @NonNull ProcessRecordKt processRecord) {
        runningProcesses.put(pid, processRecord);
    }

    @NonNull
    private ProcessRecordKt computeProcessIfAbsent(int pid, @AndroidObject Object process, int userId, int uid, String packageName) {
        return runningProcesses.computeIfAbsent(pid, key -> {
            AppInfo appInfo = computeRunningAppIfAbsent(userId, packageName, uid);
            return ProcessRecordKt.newInstance(activityManagerService, appInfo, process, pid, uid, userId, packageName);
        });
    }

    @Nullable
    public ProcessRecordKt removeRunningProcess(int pid) {
        return runningProcesses.remove(pid);
    }

    /**
     * 进程创建时的行为
     *
     * @param proc        安卓的{@link com.venus.backgroundopt.hook.constants.ClassConstants#ProcessRecord}
     * @param uid         uid
     * @param userId      userId
     * @param packageName 包名
     * @param pid         pid
     */
    public void startProcess(@AndroidObject Object proc, int uid, int userId, String packageName, int pid) {
        ConcurrentUtils.execute(activityEventChangeExecutor, throwable -> {
            getLogger().error(
                    "创建进程(userId: " + userId + ", 包名: " + packageName + "uid: " + uid + ", pid: " + pid + ")出现错误: " + throwable.getMessage(),
                    throwable
            );
            return null;
        }, () -> {
            computeProcessIfAbsent(pid, proc, userId, uid, packageName);
            return null;
        });
    }

    /**
     * 移除进程。<br>
     * 额外处理与进程相关的任务。
     *
     * @param pid pid
     */
    public void removeProcess(int pid) {
        ProcessRecordKt processRecord = getRunningProcess(pid);
        if (processRecord == null) {
            return;
        }
        AppInfo appInfo = processRecord.appInfo;
        String packageName = appInfo.getPackageName();
        boolean[] isMainProcess = {false};
        String[] processName = new String[1];
        ConcurrentUtils.execute(activityEventChangeExecutor, throwable -> {
            getLogger().error(
                    "移除进程(packageName: " + packageName + ", fullProcessName: " + processName[0] + ", userId: " + appInfo.getUserId() + ",pid: " + pid + ", isMainProcess: " + isMainProcess[0] + ")出现错误",
                    throwable
            );
            return null;
        }, () -> {
            isMainProcess[0] = processRecord.getMainProcess();
            processName[0] = processRecord.getFullProcessName();

            // 移除进程记录
            removeRunningProcess(pid);
            if (isMainProcess[0]) {
                ConcurrentUtilsKt.lock(appInfo, () -> {
                    removeRunningApp(appInfo);
                    return null;
                });
            } else {
                if (BuildConfig.DEBUG) {
                    getLogger().debug("kill: userId: " + appInfo.getUserId() + ", packageName: " + packageName + ", pid: " + pid + " >>> 子进程被杀");
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
    // 初始容量为4.一般前台app数量不会到达这个数吧?
    private final Set<AppInfo> activeAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

    // 后台分组
    private final Set<AppInfo> idleAppGroup = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Consumer<AppInfo> doNothing = appInfo -> {
    };

    private final Consumer<AppInfo> putIntoActiveAction = this::putIntoActiveAppGroup;

    private final ExecutorService activityEventChangeExecutor = Executors.newFixedThreadPool(4);

    public ExecutorService getActivityEventChangeExecutor() {
        return activityEventChangeExecutor;
    }

    /**
     * 以异步的方式处理Activity改变事件
     *
     * @param event         当前事件码
     * @param userId        用户id
     * @param componentName 组件
     */
    public void handleActivityEventChange(int event, int userId, @NonNull ComponentName componentName) {
        handleActivityEventChange(event, userId, componentName.getPackageName(), componentName);
    }

    public void handleActivityEventChange(int event, int userId, @NonNull String packageName, @Nullable ComponentName componentName) {
        ConcurrentUtils.execute(activityEventChangeExecutor, throwable -> {
            getLogger().error(
                    "处理app切换事件(userId: " + userId + "包名: " + packageName + ", event: " + event + ")错误: " + throwable.getMessage(),
                    throwable
            );
            return null;
        }, () -> {
            FindAppResult findAppResult = getFindAppResult(userId, packageName);
            AppInfo appInfo;
            if (findAppResult.getApplicationInfo() == null
                    || (appInfo = getRunningAppInfo(userId, packageName)) == null) {
                return null;
            }

            handleActivityEventChange(event, componentName, appInfo);
            return null;
        });
    }

    /**
     * 处理Activity改变事件<br>
     * 本方法处理的共有6大种情况, 本质就是从中找出切换app的方法:
     * <pre>
     *     a为第一个打开的Activity, b为第二个。
     *     1. 初次打开: event(1[a])
     *     2. 从后台到前台: event(23[a]) -> event(1[a])
     *     3. 同一app内部:
     *          1) Activity(a) -> Activity(b):  event(2[a]) -> event(1[b]) -> event(23[a])
     *          1) Activity(b) -> Activity(a):  event(2[b]) -> event(1[a]) -> event(24[b])
     *     4.   ① 退至后台/息屏?: event(2[a]) -> event(23[a])
     *          ② 点击通知进入另一个app: event(2) -> event(23)。实验情况见: /app/docs/部分场景说明/App切换事件
     *              在实验下, 可能会出现lastComponentName != curComponentName。
     *              因此, 加入ActivityManagerServiceHookKt.ACTIVITY_PAUSED,
     *              当lastComponentName != curComponentName时, 不更新当前数据
     *     5. 其他app小窗到全屏, 当前app(失去界面的app): event(1[a]) -> event(23[a])
     *     6. App关闭?: event(x[a]) -> event(24[a])
     * </pre>
     *
     * @param event         事件码
     * @param componentName 当前组件
     * @param appInfo       app
     */
    @UsageComment(
            /**
             * 在目前的逻辑实现下, 仅允许{@link com.venus.backgroundopt.hook.handle.android.ProcessListHookKt#handleSetOomAdj(XC_MethodHook.MethodHookParam)}调用
            */
            ""
    )
    public void handleActivityEventChange(int event, @Nullable ComponentName componentName, @NonNull AppInfo appInfo) {
        switch (event) {
            case ActivityManagerServiceHookKt.ACTIVITY_RESUMED -> {
                Consumer<AppInfo> consumer;
                // 从后台到前台 || 第一次打开app
                if (Objects.equals(componentName, appInfo.getComponentName())
                        /*&& appInfo.getAppSwitchEvent() == ActivityManagerServiceHookKt.ACTIVITY_STOPPED*/
                        && appInfo.getAppGroupEnum() != AppGroupEnum.ACTIVE
                        || appInfo.getComponentName() == null
                ) {
                    consumer = putIntoActiveAction;
                } else {
                    consumer = doNothing;
                }
                handleActuallyActivityEventChange(appInfo, () -> {
                    consumer.accept(appInfo);
                    updateAppSwitchState(/*event, */componentName, appInfo);
                }, throwable -> getLogger().error("ACTIVITY_RESUMED处理出错", throwable));
            }

            case ActivityManagerServiceHookKt.ACTIVITY_STOPPED -> {
                /*
                    23.10.18: appInfo.getAppGroupEnum() != AppGroupEnum.IDLE可以换成appInfo.getAppGroupEnum() == AppGroupEnum.ACTIVE,
                        但为了往后的兼容性, 暂时保持这样
                 */
                if (Objects.equals(componentName, appInfo.getComponentName())) {
                    if (appInfo.getAppGroupEnum() != AppGroupEnum.IDLE || !getPowerManager().isInteractive()) {
                        handleActuallyActivityEventChange(appInfo, () -> {
                            putIntoIdleAppGroup(appInfo);
                            updateAppSwitchState(/*event, */componentName, appInfo);
                        }, throwable -> getLogger().error("ACTIVITY_STOPPED处理出错", throwable));
                    }
                }
            }

            default -> {
            }
        }
    }

    private void handleActuallyActivityEventChange(@NonNull AppInfo appInfo, @NonNull Runnable action, @Nullable Consumer<Throwable> throwableAction) {
        Lock appInfoLock = appInfo.getLock();
        appInfoLock.lock();
        try {
            action.run();
        } catch (Throwable throwable) {
            if (throwableAction != null) {
                throwableAction.accept(throwable);
            }
        } finally {
            appInfoLock.unlock();
        }
    }

    private void updateAppSwitchState(/*int event, */ComponentName componentName, @NonNull AppInfo appInfo) {
        // 更新app的切换状态
//        appInfo.setAppSwitchEvent(event);
        appInfo.setComponentName(componentName);
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

    @Nullable
    public String getActiveLaunchPackageName() {
        return activeLaunchPackageName;
    }

    public void setActiveLaunchPackageName(String activeLaunchPackageName) {
        this.activeLaunchPackageName = activeLaunchPackageName;
    }

    public void initActiveLaunchPackageName() {
        if (packageManagerService != null && activeLaunchPackageName == null) {
            String defaultHomeName = packageManagerService.getDefaultHome();
            if (defaultHomeName != null) {
                setActiveLaunchPackageName(defaultHomeName);
                getLogger().info("默认启动器为: " + defaultHomeName);
            } else {
                getLogger().warn("初始化当前桌面失败");
            }
        }
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
    @AndroidObject(clazz = PowerManager.class)
    private PowerManager powerManager;

    public PowerManager getPowerManager() {
        if (powerManager == null) {
            powerManager = activityManagerService.getContext().getSystemService(PowerManager.class);
        }

        return powerManager;
    }

    /* *************************************************************************
     *                                                                         *
     * PackageManagerService                                                   *
     *                                                                         *
     **************************************************************************/
    private PackageManagerService packageManagerService;

    public PackageManagerService getPackageManagerService() {
        return packageManagerService;
    }

    public void setPackageManagerService(PackageManagerService packageManagerService) {
        this.packageManagerService = packageManagerService;
    }
}