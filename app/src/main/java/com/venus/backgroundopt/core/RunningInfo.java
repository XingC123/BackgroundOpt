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

import android.app.role.RoleManager;
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
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerShellCommand;
import com.venus.backgroundopt.hook.handle.android.entity.MemInfoReader;
import com.venus.backgroundopt.hook.handle.android.entity.PackageManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt;
import com.venus.backgroundopt.manager.application.DefaultApplicationManager;
import com.venus.backgroundopt.manager.process.ProcessManager;
import com.venus.backgroundopt.reference.PropertyChangeListener;
import com.venus.backgroundopt.service.ProcessDaemonService;
import com.venus.backgroundopt.utils.ThrowableUtilsKt;
import com.venus.backgroundopt.utils.concurrent.ConcurrentUtils;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

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
            return new AppInfo(
                    userId,
                    packageName,
                    getFindAppResult(userId, packageName),
                    this
            ).setUid(uid);
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
            getLogger().warn("kill: 包名为空");
            return;
        }

        ConcurrentUtils.execute(activityEventChangeExecutor, throwable -> {
            getLogger().error(
                    "杀死app(packageName: " + packageName + ", userId: " + appInfo.getUserId() + ")出现错误",
                    throwable
            );
            return null;
        }, () -> {
            /*ConcurrentUtilsKt.lock(appInfo, () -> {*/
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
                /*return null;
            });*/
            return null;
        });
    }

    public void forceStopRunningApp(@NonNull AppInfo appInfo) {
        String packageName = appInfo.getPackageName();
        if (packageName == null) {
            return;
        }
        ConcurrentUtils.execute(activityEventChangeExecutor, () -> {
            /*ConcurrentUtilsKt.lock(appInfo, () -> {*/
            ThrowableUtilsKt.runCatchThrowable(null, throwable -> {
                getLogger().error("强制停止app出错(uid: " + appInfo.getUid() + ", packageName: " + packageName + ")", throwable);
                return null;
            }, null, () -> {
                activityManagerService.forceStopPackage(packageName, appInfo.getUserId());
                return null;
            });
                /*return null;
            });*/
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
        ProcessRecordKt processRecord = getRunningProcess(pid);
        if (processRecord == null) {
            return;
        }
        AppInfo appInfo = processRecord.appInfo;
        String packageName = appInfo.getPackageName();
        boolean isMainProcess = processRecord.getMainProcess();

        // 移除进程记录
        removeRunningProcess(pid);
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
        /*ConcurrentUtils.execute(activityEventChangeExecutor, throwable -> {
            getLogger().error(
                    "处理app切换事件(userId: " + userId + "包名: " + packageName + ", event: " + event + ")错误: " + throwable.getMessage(),
                    throwable
            );
            return null;
        }, () -> {*/
        FindAppResult findAppResult = getFindAppResult(userId, packageName);
        AppInfo appInfo;
        if (findAppResult.getApplicationInfo() == null
                || (appInfo = getRunningAppInfo(userId, packageName)) == null) {
            return /*null*/;
        }

        handleActivityEventChange(event, componentName, appInfo);
            /*return null;
        });*/
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
    private final DefaultApplicationManager defaultApplicationManager = new DefaultApplicationManager();

    public void setDefaultPackageName(String key, String packageName) {
        defaultApplicationManager.setDefaultPackageName(key, packageName);
    }

    public String getDefaultPackageName(String key) {
        return defaultApplicationManager.getDefaultPackageName(key);
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
                .forEach(appInfo -> appInfo.shouldHandleAdj = AppInfo.handleAdjDependOnAppOptimizePolicy);
        // 保存新桌面包名
        this.activeLaunchPackageName = activeLaunchPackageName;
    }

    public void initActiveDefaultAppPackageName() {
        if (packageManagerService != null) {
            // 以下数组的索引一一对应
            String[] keys = {
                    DefaultApplicationManager.DEFAULT_APP_BROWSER,
                    DefaultApplicationManager.DEFAULT_APP_HOME,
                    DefaultApplicationManager.DEFAULT_APP_ASSISTANT,
                    DefaultApplicationManager.DEFAULT_APP_INPUT_METHOD,
            };
            Supplier<String>[] suppliers = new Supplier[]{
                    packageManagerService::getDefaultBrowser,
                    packageManagerService::getDefaultHome,
                    packageManagerService::getDefaultAssistant,
                    packageManagerService::getDefaultInputMethod
            };
            String[] tags = {
                    "浏览器",
                    "桌面",
                    "智能助手",
                    "输入法",
            };
            for (int i = 0; i < keys.length; i++) {
                int finalI = i;
                defaultApplicationManager.initDefaultApplicationNode(
                        keys[finalI],
                        defaultApplicationNode -> {
                            initDefaultApplicationNode(
                                    defaultApplicationNode,
                                    suppliers[finalI],
                                    keys[finalI],
                                    tags[finalI],
                                    generateDefaultApplicationChangeListener(tags[finalI])
                            );
                            return null;
                        }
                );
            }
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
            String newPkgName = DefaultApplicationManager.getDefaultPkgNameFromUriStr(newValue);
            if (Objects.equals(oldValue, newPkgName)) {
                return;
            }
            runningApps.values().stream()
                    .filter(AppInfo::isImportSystemApp)
                    .filter(appInfo -> Objects.equals(appInfo.getPackageName(), oldValue) || Objects.equals(appInfo.getPackageName(), newPkgName))
                    .forEach(appInfo -> {
                        if (Objects.equals(appInfo.getPackageName(), oldValue)) {
                            // 将原应用恢复默认策略
                            appInfo.shouldHandleAdj = AppInfo.handleAdjDependOnAppOptimizePolicy;
                        } else {
                            // 设置当前app
                            appInfo.shouldHandleAdj = AppInfo.handleAdjAlways;
                        }
                    });
            getLogger().info("更换的" + tag + "包名为: " + newPkgName);
        };
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
     * ActivityManagerShellCommand                                             *
     *                                                                         *
     **************************************************************************/
    private ActivityManagerShellCommand activityManagerShellCommand;

    public ActivityManagerShellCommand getActivityManagerShellCommand() {
        return activityManagerShellCommand;
    }

    public void setActivityManagerShellCommand(ActivityManagerShellCommand activityManagerShellCommand) {
        this.activityManagerShellCommand = activityManagerShellCommand;
    }
}