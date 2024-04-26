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

package com.venus.backgroundopt.entity;

import static com.venus.backgroundopt.core.RunningInfo.AppGroupEnum;

import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.environment.hook.HookCommonProperties;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.manager.application.DefaultApplicationManager;
import com.venus.backgroundopt.utils.concurrent.lock.LockFlag;
import com.venus.backgroundopt.utils.log.ILogger;
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * app信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class AppInfo implements ILogger, LockFlag {
    /* *************************************************************************
     *                                                                         *
     * app基本信息                                                               *
     *                                                                         *
     **************************************************************************/
    /**
     * 多开uid:
     * miui: 99910435 = 用户id+真正uid
     * 安卓13原生: 1010207 = 用户id+真正uid
     */
    private int uid = Integer.MIN_VALUE;
    private String packageName;
    private int userId = Integer.MIN_VALUE;

    public AppInfo(int userId, String packageName, FindAppResult findAppResult, RunningInfo runningInfo) {
        this.userId = userId;
        this.packageName = packageName;
        this.findAppResult = findAppResult;
        this.runningInfo = runningInfo;

        setShouldHandleAdj();
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息                                                               *
     *                                                                         *
     **************************************************************************/
    @SuppressWarnings("all")    // 别显示未final啦, 烦死辣
    private RunningInfo runningInfo;
    private volatile ProcessRecord mProcessRecord;   // 主进程记录

    // 当前app被模块检测到的activity的ComponentName
    @SuppressWarnings("all")
    private Set<ComponentName> activities = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

    private final AtomicInteger appSwitchEvent = new AtomicInteger(Integer.MIN_VALUE); // app切换事件

    private final AtomicBoolean switchEventHandled = new AtomicBoolean(false);    // 切换事件处理完毕

    private final AtomicReference<ComponentName> componentNameAtomicReference = new AtomicReference<>(null);

    private FindAppResult findAppResult;

    @Nullable
    public FindAppResult getFindAppResult() {
        return findAppResult;
    }

    public void setFindAppResult(@Nullable FindAppResult findAppResult) {
        this.findAppResult = findAppResult;
    }

    public boolean isImportSystemApp() {
        return findAppResult.getImportantSystemApp();
    }

    public boolean isSwitchEventHandled() {
        return switchEventHandled.get();
    }

    public void setSwitchEventHandled(boolean switchEventHandled) {
        this.switchEventHandled.set(switchEventHandled);
    }

    public ComponentName getComponentName() {
        return componentNameAtomicReference.get();
    }

    public void setComponentName(ComponentName componentName) {
        componentNameAtomicReference.set(componentName);
    }

    public void activityActive(@NonNull ComponentName componentName) {
        activities.add(componentName);
    }

    public void activityDie(@NonNull ComponentName componentName) {
        activities.remove(componentName);
    }

    public int getAppShowingActivityCount() {
        return activities.size();
    }

    public boolean hasActivity() {
        return getAppShowingActivityCount() >= 1;
    }

    /* *************************************************************************
     *                                                                         *
     * adj处理                                                                  *
     *                                                                         *
     **************************************************************************/
    public static final Function<AppOptimizePolicy, Boolean> handleAdjDependOnAppOptimizePolicy = appOptimizePolicy -> {
        if (appOptimizePolicy == null) {
            return false;
        }
        return Boolean.TRUE.equals(appOptimizePolicy.getShouldHandleAdj());
    };

    public static final Function<AppOptimizePolicy, Boolean> handleAdjAlways = appOptimizePolicy -> true;

    public static final Function<AppOptimizePolicy, Boolean> handleAdjNever = appOptimizePolicy -> false;

    public volatile Function<AppOptimizePolicy, Boolean> shouldHandleAdj = AppInfo.handleAdjAlways;

    /**
     * 应用是否需要管理adj
     *
     * @return 需要 -> true
     */
    public boolean shouldHandleAdj() {
        return shouldHandleAdj.apply(HookCommonProperties.INSTANCE.getAppOptimizePolicyMap().get(packageName));
    }

    public boolean shouldHandleAdj(AppOptimizePolicy appOptimizePolicy) {
        return shouldHandleAdj.apply(appOptimizePolicy);
    }

    public boolean setShouldHandleAdj() {
        return setShouldHandleAdj(HookCommonProperties.INSTANCE.getAppOptimizePolicyMap().get(packageName));
    }

    public boolean setShouldHandleAdj(@Nullable AppOptimizePolicy appOptimizePolicy) {
        boolean shouldHandleAdjUiState = false;
        // 未配置app优化策略
        if (appOptimizePolicy == null) {
            // 系统app默认不管理
            if (shouldHandleAdj(findAppResult, packageName)) {
                // 用户app默认管理adj
                this.shouldHandleAdj = AppInfo.handleAdjAlways;
                shouldHandleAdjUiState = true;
            } else {
                this.shouldHandleAdj = AppInfo.handleAdjNever;
            }
        }
        // 已配置app优化策略 或者 打开过app配置页
        else if (Boolean.TRUE.equals(appOptimizePolicy.getShouldHandleAdj())
                || (appOptimizePolicy.getShouldHandleAdj() == null && shouldHandleAdj(findAppResult, packageName))
        ) {
            this.shouldHandleAdj = AppInfo.handleAdjAlways;
            shouldHandleAdjUiState = true;
        } else {
            this.shouldHandleAdj = AppInfo.handleAdjNever;
        }

        // 如果现在是永不处理
        if (this.shouldHandleAdj == AppInfo.handleAdjNever) {
            runningInfo.getRunningProcesses().stream()
                    // 按包名匹配(处理应用分身情况)
                    .filter(processRecord -> Objects.equals(processRecord.getPackageName(), packageName))
                    .forEach(ProcessRecord::resetMaxAdj);
        }

        return shouldHandleAdjUiState;
    }

    public static boolean shouldHandleAdj(@NonNull FindAppResult findAppResult, String packageName) {
        // 系统app默认不管理
        // 用户app默认管理adj
        // 默认app默认管理adj
        return !(findAppResult.getImportantSystemApp() && !DefaultApplicationManager.isDefaultAppPkgName(packageName));
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息(简单)                                                         *
     *                                                                         *
     **************************************************************************/

    // 当前app在本模块内的内存分组
    private volatile AppGroupEnum appGroupEnum = AppGroupEnum.NONE;

    public void setAppGroupEnum(AppGroupEnum appGroupEnum) {
//        synchronized (appGroupSetLock) {
//            this.appGroupEnum = appGroupEnum;
//            // 添加 切后台时所创建的所有进程(当app进入后台后, 新创建的子进程会自动被添加到待压缩列表中)
//            if (!Objects.equals(runningInfo.getActiveLaunchPackageName(), packageName)) {   // 不是桌面进程
//                switch (appGroupEnum) {
//                    case IDLE -> runningInfo.getProcessManager().addCompactProcess(getProcesses());
//                    case ACTIVE ->  /* 若为第一次打开app, 此时也会执行这里。此时无需考虑是否能移除, 因为还没开始添加(至少也要在app第一次进入后台才会开始添加) */
//                            runningInfo.getProcessManager().cancelCompactProcess(getProcesses());
//                }
//            }
//        }
        this.appGroupEnum = appGroupEnum;
    }

    public AppGroupEnum getAppGroupEnum() {
        return appGroupEnum;
    }

    public ApplicationIdentity getApplicationIdentity() {
        return new ApplicationIdentity(userId, packageName);
    }

    /**
     * 获取主进程当前记录的adj
     *
     * @return 主进程当前记录的adj
     */
    public int getMainProcCurAdj() {
        return this.mProcessRecord.getFixedOomAdjScore();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return uid == appInfo.uid && Objects.equals(packageName, appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, packageName);
    }

    @NonNull
    @Override
    public String toString() {
        return "AppInfo{" +
                "uid=" + uid +
                ", packageName='" + packageName + '\'' +
                ", userId=" + userId +
                ", appGroupEnum=" + appGroupEnum +
                ", mPid=" + getmPid() +
                '}';
    }

    /* *************************************************************************
     *                                                                         *
     * 当前appInfo清理状态                                                       *
     *                                                                         *
     **************************************************************************/
    private static final Field[] fields;

    static {
        fields = Arrays.stream(AppInfo.class.getDeclaredFields())
                .filter(field -> !(field.getType().isPrimitive() || field.getType() == Field[].class || Modifier.isStatic(field.getModifiers())))
                .peek(field -> field.setAccessible(true))
                .toArray(Field[]::new);
    }

    /**
     * jvm的可达性分析会回收对象的。此方法只是确保在调用后将对象内引用指针置空, 当发生可能的对某属性执行获取操作时会收到异常。
     */
    public void clearAppInfo() {
        try {
            Object obj;
            for (Field field : fields) {
                obj = field.get(this);
                if (obj instanceof Map<?, ?> map) {
                    map.clear();
                } else if (obj instanceof Collection<?> collection) {
                    collection.clear();
                } else if (obj instanceof AtomicReference<?> ap) {
                    ap.set(null);
                }

                if (!(obj instanceof AppGroupEnum || obj instanceof Lock || obj instanceof FindAppResult)) {
                    field.set(this, null);
                }
            }
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {    // 正式发布版无需关注这里
                getLogger().error("AppInfo清理失败", t);
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * getter/setter                                                           *
     *                                                                         *
     **************************************************************************/
    public static int getRepairedUid(int userId, int uid) {
        if (userId == ActivityManagerService.MAIN_USER) {
            return uid;
        } else {    // 还需判断传入的uid是否已经是符合"用户id+uid"
            int compute = uid / ActivityManagerService.USER_APP_UID_START_NUM;

            // 0: 系统应用, 1: 用户程序
            // 位数不够需要补0(测试于: MIUI13 22.7.8 安卓12)
            if (compute == 0) {
                return Integer.parseInt(userId + String.valueOf(0) + uid);
            } else {
                return Integer.parseInt(String.valueOf(userId) + uid);
            }
        }
    }

    public int getUid() {
        return uid;
    }

    public AppInfo setUid(int uid) {
        this.uid = uid;

        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getmPid() {
        return this.mProcessRecord.getPid();
//        return this.mProcessInfo.getPid();
//        if (this.mProcessRecord == null) {
//            return Integer.MIN_VALUE;
//        } else {
//            return this.mProcessRecord.getPid();
//        }
    }

    public int getAppSwitchEvent() {
        return appSwitchEvent.get();
    }

    public void setAppSwitchEvent(int appSwitchEvent) {
        this.appSwitchEvent.set(appSwitchEvent);

        if (BuildConfig.DEBUG) {
            getLogger().debug(packageName + " 切换状态 ->>> " + appSwitchEvent);
        }
    }

    @Nullable
    public ProcessRecord getmProcessRecord() {
        return mProcessRecord;
    }

    public void setmProcessRecord(ProcessRecord mProcessRecord) {
        this.mProcessRecord = mProcessRecord;

        if (BuildConfig.DEBUG) {
            getLogger().debug("设置ProcessRecord(" + mProcessRecord.getPackageName() + "-" + mProcessRecord.getPid() + ")");
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 类锁                                                                     *
     *                                                                         *
     **************************************************************************/
    @SuppressWarnings("all")
    private ReentrantLock lock = new ReentrantLock(true);

    @NonNull
    @Override
    public Lock getLock() {
        return lock;
    }
}