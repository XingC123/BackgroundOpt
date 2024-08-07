/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.core;

import static com.venus.backgroundopt.manager.application.DefaultApplicationManager.DefaultApplicationNode;

import android.content.ComponentName;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.FindAppResult;
import com.venus.backgroundopt.entity.FindAppResultKt;
import com.venus.backgroundopt.environment.hook.HookCommonProperties;
import com.venus.backgroundopt.hook.base.IHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt;
import com.venus.backgroundopt.hook.handle.android.ProcessListHookKt;
import com.venus.backgroundopt.hook.handle.android.ProcessListHookKtKt;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.MemInfoReader;
import com.venus.backgroundopt.hook.handle.android.entity.PackageManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord.AdjHandleActionType;
import com.venus.backgroundopt.manager.application.DefaultApplicationManager;
import com.venus.backgroundopt.manager.message.ModuleMessageManager;
import com.venus.backgroundopt.manager.process.ProcessManager;
import com.venus.backgroundopt.reference.PropertyChangeListener;
import com.venus.backgroundopt.service.ProcessDaemonService;
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtilsKt;
import com.venus.backgroundopt.utils.log.ILogger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        return getFindAppResult(
                getNormalAppKey(userId, packageName),
                userId,
                packageName
        );
    }

    @NonNull
    private FindAppResult getFindAppResult(String key, int userId, String packageName) {
        return findAppResultMap.computeIfAbsent(
                key,
                k -> activityManagerService.getFindAppResult(userId, packageName)
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
        return getAppKey(userId, packageName);
    }

    private String getAppKey(int userId, String packageName) {
        return userId == ActivityManagerService.MAIN_USER ? packageName : userId + ":" + packageName;
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
        return getAppKey(userId, packageName);
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

    @Nullable
    private AppInfo getRunningAppInfo(String key) {
        return runningApps.get(key);
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
            return FindAppResultKt.getOrCreateAppInfo(getFindAppResult(userId, packageName), result -> {
                return AppInfo.newInstance(
                        uid,
                        userId,
                        packageName,
                        result,
                        this
                );
            });
        });
    }

    /**
     * 根据{@link AppInfo}从运行列表中移除
     *
     * @param appInfo app信息
     */
    public void removeRunningApp(@NonNull AppInfo appInfo) {
        // 设置内存分组到死亡分组
        appInfo.setAppGroupEnum(AppGroupEnum.DEAD);

        String packageName = appInfo.getPackageName();
        if (packageName == null) {
            getLogger().warn("kill: 包名为空(uid: " + appInfo.getUid() + ")");
            return;
        }

        try {
            // 从运行列表移除
            AppInfo remove = runningApps.remove(getRunningAppIdentifier(appInfo.getUserId(), packageName));
            if (remove != null) {
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
        } catch (Throwable throwable) {
            getLogger().error(
                    "杀死app(packageName: " + packageName + ", userId: " + appInfo.getUserId() + ")出现错误",
                    throwable
            );
        }
    }

    public void forceStopRunningApp(@NonNull AppInfo appInfo) {
        String packageName = appInfo.getPackageName();
        if (packageName == null) {
            return;
        }
        try {
            activityManagerService.forceStopPackage(packageName, appInfo.getUserId());
        } catch (Throwable throwable) {
            getLogger().error("强制停止app出错(uid: " + appInfo.getUid() + ", packageName: " + packageName + ")", throwable);
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的进程                                                                *
     *                                                                         *
     **************************************************************************/
    private final Map<Integer, ProcessRecord> runningProcesses = new ConcurrentHashMap<>();

    @Nullable
    public ProcessRecord getRunningProcess(int pid) {
        return runningProcesses.get(pid);
    }

    @NonNull
    public Collection<ProcessRecord> getRunningProcesses() {
        return runningProcesses.values();
    }

    private void putIntoRunningProcesses(int pid, @NonNull ProcessRecord processRecord) {
        runningProcesses.put(pid, processRecord);
    }

    @NonNull
    private ProcessRecord computeProcessIfAbsent(int pid, @AndroidObject Object process, int userId, int uid, String packageName) {
        return runningProcesses.computeIfAbsent(pid, key -> {
            AppInfo appInfo = computeRunningAppIfAbsent(userId, packageName, uid);
            return ProcessRecord.newInstance(activityManagerService, appInfo, process, pid, uid, userId, packageName);
        });
    }

    @Nullable
    public ProcessRecord removeRunningProcess(int pid) {
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
        /*ConcurrentUtils.execute(activityEventChangeExecutor, throwable -> {
            getLogger().error(
                    "创建进程(userId: " + userId + ", 包名: " + packageName + "uid: " + uid + ", pid: " + pid + ")出现错误: " + throwable.getMessage(),
                    throwable
            );
            return null;
        }, () -> {*/
        computeProcessIfAbsent(pid, proc, userId, uid, packageName);
            /*return null;
        });*/
    }

    /**
     * 移除进程。<br>
     * 额外处理与进程相关的任务。
     *
     * @param pid pid
     */
    public void removeProcess(int pid) {
        ProcessRecord processRecord = getRunningProcess(pid);
        if (processRecord == null) {
            return;
        }
        AppInfo appInfo = processRecord.appInfo;
        String packageName = appInfo.getPackageName();
        boolean isMainProcess = processRecord.getMainProcess();

        // 移除进程记录
        removeRunningProcess(pid);
        // 移除内存压缩文件流的缓存
        activityManagerService.getOomAdjuster().getCachedAppOptimizer().removeCompactOutputStreams(pid);

        if (isMainProcess) {
            removeRunningApp(appInfo);
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().debug("kill: userId: " + appInfo.getUserId() + ", packageName: " + packageName + ", pid: " + pid + " >>> 子进程被杀");
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

    private final ExecutorService activityEventChangeExecutor = Executors.newFixedThreadPool(3);

    public ExecutorService getActivityEventChangeExecutor() {
        return activityEventChangeExecutor;
    }

    public void addActivityEventChangeAction(Runnable block) {
        getActivityEventChangeExecutor().execute(block);
    }

    /**
     * 以异步的方式处理Activity改变事件
     *
     * @param event         当前事件码
     * @param userId        用户id
     * @param componentName 组件
     */
    public void handleActivityEventChange(int event, int userId, @NonNull ComponentName componentName) {
        addActivityEventChangeAction(() -> {
            handleActivityEventChange(event, userId, componentName.getPackageName(), componentName);
        });
    }

    public void handleActivityEventChange(int event, int userId, @NonNull String packageName, @Nullable ComponentName componentName) {
        String appKey = getAppKey(userId, packageName);
        FindAppResult findAppResult = getFindAppResult(appKey, userId, packageName);
        AppInfo appInfo;
        if ((appInfo = getRunningAppInfo(appKey)) == null || findAppResult.getApplicationInfo() == null) {
            return;
        }

        ConcurrentUtilsKt.lock(appInfo, () -> {
            handleActivityEventChangeLocked(event, componentName, appInfo);
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
    public void handleActivityEventChangeLocked(int event, @Nullable ComponentName componentName, @NonNull AppInfo appInfo) {
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
                    appInfo.activityActive(componentName);

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

            case ActivityManagerServiceHookKt.ACTIVITY_DESTROYED -> {
                appInfo.activityDie(componentName);
            }

            default -> {
            }
        }
    }

    private void handleActuallyActivityEventChange(@NonNull AppInfo appInfo, @NonNull Runnable action, @Nullable Consumer<Throwable> throwableAction) {
        /*Lock appInfoLock = appInfo.getLock();
        appInfoLock.lock();*/
        try {
            action.run();
        } catch (Throwable throwable) {
            if (throwableAction != null) {
                throwableAction.accept(throwable);
            }
        }/* finally {
            appInfoLock.unlock();
        }*/
    }

    private void updateAppSwitchState(/*int event, */ComponentName componentName, @NonNull AppInfo appInfo) {
        // 更新app的切换状态
//        appInfo.setAppSwitchEvent(event);
        appInfo.setComponentName(componentName);
    }

    private void putIntoActiveAppGroup(@NonNull AppInfo appInfo) {
        activeAppGroup.add(appInfo);
        appInfo.setAppGroupEnum(AppGroupEnum.ACTIVE);

        // 处理当前app
        handleCurApp(appInfo);

        // 重置切换事件处理状态
        appInfo.setSwitchEventHandled(false);

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + ", uid: " + appInfo.getUid() + " 被放入ActiveGroup");
        }
    }

    private void putIntoIdleAppGroup(@NonNull AppInfo appInfo) {
        idleAppGroup.add(appInfo);
        appInfo.setAppGroupEnum(AppGroupEnum.IDLE);

        // 处理上个app
        handleLastApp(appInfo);

        appInfo.setSwitchEventHandled(true);

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

        ProcessRecord processRecord = appInfo.getmProcessRecord();
        if (processRecord != null) {
            if (processRecord.getAdjHandleActionType() == AdjHandleActionType.CUSTOM_MAIN_PROCESS) {
                ProcessListHookKt hookInstance = IHook.getHookInstance(ProcessListHookKt.class);
                if (hookInstance != null) {
                    hookInstance.handleSetOomAdjLocked(
                            processRecord,
                            ProcessList.FOREGROUND_APP_ADJ,
                            HookCommonProperties.getGlobalOomScorePolicy().getValue()
                    );
                }
            }
        }
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

        // 默认对高优先级子进程设置adj
        ProcessListHookKt hookInstance = IHook.getHookInstance(ProcessListHookKt.class);
        if (hookInstance != null) {
            int adj = hookInstance.getOomAdjHandler().computeHighPrioritySubProcessAdj(0);
            //hookInstance.addAdjSetAction(() -> {
            getRunningProcesses().stream()
                    .filter(processRecord -> processRecord.appInfo == appInfo)
                    .filter(processRecord -> !processRecord.getMainProcess())
                    .filter(ProcessListHookKtKt::isHighPrioritySubProcess)
                    .forEach(processRecord -> {
                        hookInstance.addAdjComputeAndApplyAction(processRecord, () -> {
                            ProcessList.writeLmkd(
                                    processRecord.getPid(),
                                    processRecord.getUid(),
                                    adj
                            );
                            return null;
                        });
                    });
            //});
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 默认桌面                                                                  *
     *                                                                         *
     **************************************************************************/
    private final DefaultApplicationManager defaultApplicationManager = new DefaultApplicationManager();

    public void setDefaultPackageName(String key, String packageName) {
        defaultApplicationManager.setDefaultPackageName(key, packageName);
    }

    public String getDefaultPackageName(String key) {
        return defaultApplicationManager.getDefaultPackageName(key);
    }

    public Collection<String> getAllDefaultPackageNames() {
        return defaultApplicationManager.getAllPkgNames();
    }

    @Deprecated
    private volatile String activeLaunchPackageName = null;

    @Nullable
    public String getActiveLaunchPackageName() {
        return getDefaultPackageName(DefaultApplicationManager.DEFAULT_APP_HOME);
    }

    @Deprecated
    public void setActiveLaunchPackageName(String activeLaunchPackageName) {
        String curLaunchPackageName = this.activeLaunchPackageName;
        // 将原桌面的adj调节修改
        runningApps.values().stream()
                .filter(appInfo -> Objects.equals(appInfo.getPackageName(), curLaunchPackageName))
                .forEach(appInfo -> appInfo.adjHandleFunction = AppInfo.handleAdjDependOnAppOptimizePolicy);
        // 保存新桌面包名
        this.activeLaunchPackageName = activeLaunchPackageName;
    }

    public void initActiveDefaultAppPackageName() {
        if (packageManagerService != null) {
            List.of(
                    /*new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_BROWSER)
                            .setTag("浏览器")
                            .setPkgNameGetter(packageManagerService::getDefaultBrowser),*/
                    new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_HOME)
                            .setTag("桌面")
                            .setPkgNameGetter(packageManagerService::getDefaultHome),
                    new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_ASSISTANT)
                            .setTag("智能助手")
                            .setPkgNameGetter(packageManagerService::getDefaultAssistant),
                    new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_INPUT_METHOD)
                            .setTag("输入法")
                            .setPkgNameGetter(packageManagerService::getDefaultInputMethod),
                    new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_DIALER)
                            .setTag("通讯录与拨号")
                            .setPkgNameGetter(packageManagerService::getDefaultDialer),
                    new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_SMS)
                            .setTag("短信")
                            .setPkgNameGetter(packageManagerService::getDefaultSms)
            ).forEach(initializer -> {
                defaultApplicationManager.initDefaultApplicationNode(
                        initializer.key,
                        defaultApplicationNode -> {
                            initDefaultApplicationNode(
                                    defaultApplicationNode,
                                    initializer.pkgNameGetter,
                                    initializer.key,
                                    initializer.tag,
                                    generateDefaultApplicationChangeListener(initializer.tag)
                            );
                            return null;
                        }
                );
            });
        }
    }

    private void initDefaultApplicationNode(
            DefaultApplicationNode defaultApplicationNode,
            Supplier<String> defaultPkgNameSupplier,
            String key,
            String tag,
            PropertyChangeListener<String> listener
    ) {
        String defaultPkgName = defaultPkgNameSupplier.get();
        defaultApplicationNode.setValue(defaultPkgName);
        defaultApplicationNode.addListener(key, listener);
        if (defaultPkgName != null) {
            getLogger().info("默认" + tag + "为: " + defaultPkgName);
        } else {
            getLogger().warn("获取默认" + tag + "失败");
        }
    }

    private PropertyChangeListener<String> generateDefaultApplicationChangeListener(String tag) {
        return (oldValue, newValue) -> {
            runningApps.values().stream()
                    // .filter(AppInfo::isImportSystemApp)
                    .filter(appInfo -> Objects.equals(appInfo.getPackageName(), oldValue) || Objects.equals(appInfo.getPackageName(), newValue))
                    .forEach(appInfo -> {
                        if (Objects.equals(appInfo.getPackageName(), oldValue)) {
                            appInfo.setAdjHandleFunction();
                        } else {
                            // 设置当前app
                            appInfo.adjHandleFunction = AppInfo.handleAdjAlways;
                        }
                    });
            getLogger().info("更换的" + tag + "包名为: " + newValue);
        };
    }

    /**
     * 用于初始化默认应用的包名的事件节点
     */
    private static class DefaultApplicationPkgNameInitializer {
        String key;
        String tag;
        Supplier<String> pkgNameGetter;

        public DefaultApplicationPkgNameInitializer setKey(String key) {
            this.key = key;
            return this;
        }

        public DefaultApplicationPkgNameInitializer setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public DefaultApplicationPkgNameInitializer setPkgNameGetter(Supplier<String> pkgNameGetter) {
            this.pkgNameGetter = pkgNameGetter;
            return this;
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

    /* *************************************************************************
     *                                                                         *
     * 模块消息管理器                                                             *
     *                                                                         *
     **************************************************************************/
    private final ModuleMessageManager moduleMessageManager = new ModuleMessageManager(this);

    {
        moduleMessageManager.start();
    }

    public ModuleMessageManager getModuleMessageManager() {
        return moduleMessageManager;
    }
}
