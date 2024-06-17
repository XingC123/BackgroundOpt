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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
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
import java.util.function.BiFunction;

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
    @DontClearField
    private int uid = Integer.MIN_VALUE;
    @DontClearField
    private String packageName;
    @DontClearField
    private int userId = Integer.MIN_VALUE;

    private AppInfo(int userId, String packageName, FindAppResult findAppResult, RunningInfo runningInfo) {
        this.userId = userId;
        this.packageName = packageName;
        this.findAppResult = findAppResult;
        this.runningInfo = runningInfo;
    }

    public AppInfo init() {
        appSwitchEvent.set(Integer.MIN_VALUE);
        switchEventHandled.set(false);
        componentNameAtomicReference.set(null);
        appGroupEnum = AppGroupEnum.NONE;

        setAdjHandleFunction();

        return this;
    }

    public static AppInfo newInstance(
            int uid,
            int userId,
            String packageName,
            FindAppResult findAppResult,
            RunningInfo runningInfo
    ) {
        return new AppInfo(
                userId,
                packageName,
                findAppResult,
                runningInfo
        ).setUid(uid);
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息                                                               *
     *                                                                         *
     **************************************************************************/
    @DontClearField
    private final RunningInfo runningInfo;
    private volatile ProcessRecord mProcessRecord;   // 主进程记录

    // 当前app被模块检测到的activity的ComponentName
    @DontClearField(reset = true)
    private final Set<ComponentName> activities = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

    @DontClearField
    private final AtomicInteger appSwitchEvent = new AtomicInteger(Integer.MIN_VALUE); // app切换事件

    @DontClearField(reset = true)
    private final AtomicBoolean switchEventHandled = new AtomicBoolean(false);    // 切换事件处理完毕

    @DontClearField(reset = true)
    private final AtomicReference<ComponentName> componentNameAtomicReference = new AtomicReference<>(null);

    @DontClearField
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
    @DontClearField
    @Deprecated
    public static final BiFunction<AppInfo, AppOptimizePolicy, Boolean> handleAdjDependOnAppOptimizePolicy = (appInfo, appOptimizePolicy) -> {
        if (appOptimizePolicy == null) {
            return appInfo.hasActivity();
        }
        return switch (appOptimizePolicy.getMainProcessAdjManagePolicy()) {
            case MAIN_PROC_ADJ_MANAGE_ALWAYS -> true;
            case MAIN_PROC_ADJ_MANAGE_HAS_ACTIVITY -> appInfo.hasActivity();
            case MAIN_PROC_ADJ_MANAGE_NEVER -> false;
            default -> appInfo.hasActivity();
        };
    };

    @DontClearField
    public static final BiFunction<AppInfo, AppOptimizePolicy, Boolean> handleAdjAlways = (appInfo, appOptimizePolicy) -> true;

    @DontClearField
    public static final BiFunction<AppInfo, AppOptimizePolicy, Boolean> handleAdjNever = (appInfo, appOptimizePolicy) -> false;

    @DontClearField
    public static final BiFunction<AppInfo, AppOptimizePolicy, Boolean> handleAdjIfHasActivity = (appInfo, appOptimizePolicy) -> appInfo.hasActivity();

    @DontClearField
    public volatile BiFunction<AppInfo, AppOptimizePolicy, Boolean> adjHandleFunction = AppInfo.handleAdjIfHasActivity;

    /**
     * 应用是否需要管理adj
     *
     * @return 需要 -> true
     */
    public boolean shouldHandleAdj() {
        return adjHandleFunction.apply(
                this,
                HookCommonProperties.INSTANCE.getAppOptimizePolicyMap().get(packageName)
        );
    }

    public boolean shouldHandleAdj(AppOptimizePolicy appOptimizePolicy) {
        return adjHandleFunction.apply(this, appOptimizePolicy);
    }

    public void setAdjHandleFunction() {
        setAdjHandleFunction(HookCommonProperties.INSTANCE.getAppOptimizePolicyMap().get(packageName));
    }

    public void setAdjHandleFunction(@Nullable AppOptimizePolicy appOptimizePolicy) {
        if (appOptimizePolicy == null) {
            this.adjHandleFunction = handleAdjIfHasActivity;
        }
        // 已配置app优化策略
        else {
            this.adjHandleFunction = switch (appOptimizePolicy.getMainProcessAdjManagePolicy()) {
                case MAIN_PROC_ADJ_MANAGE_NEVER -> handleAdjNever;
                case MAIN_PROC_ADJ_MANAGE_ALWAYS -> handleAdjAlways;
                case MAIN_PROC_ADJ_MANAGE_HAS_ACTIVITY -> handleAdjIfHasActivity;
                // 容错
                default -> handleAdjIfHasActivity;
            };
        }

        // 默认应用, 始终处理adj
        // 即便已配置过app优化策略
        if (DefaultApplicationManager.isDefaultAppPkgName(packageName)) {
            this.adjHandleFunction = handleAdjAlways;
        }

        // 如果现在是永不处理
        if (this.adjHandleFunction == AppInfo.handleAdjNever) {
            runningInfo.getRunningProcesses().stream()
                    // 按包名匹配(处理应用分身情况)
                    .filter(processRecord -> Objects.equals(processRecord.getPackageName(), packageName))
                    .forEach(ProcessRecord::resetMaxAdj);
        }
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息(简单)                                                         *
     *                                                                         *
     **************************************************************************/
    // 当前app在本模块内的内存分组
    @DontClearField
    private volatile AppGroupEnum appGroupEnum = AppGroupEnum.NONE;

    public void setAppGroupEnum(AppGroupEnum appGroupEnum) {
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
    @DontClearField
    private static final Field[] fields;

    static {
        fields = Arrays.stream(AppInfo.class.getDeclaredFields())
                .filter(field -> {
                    DontClearField dontClearField = field.getAnnotation(DontClearField.class);
                    return dontClearField == null || dontClearField.reset();
                })
                .peek(field -> field.setAccessible(true))
                .toArray(Field[]::new);
    }

    /**
     * jvm的可达性分析会回收对象的。此方法只是确保在调用后将对象内引用指针置空, 当发生可能的对某属性执行获取操作时会收到异常。
     */
    public void clearAppInfo() {
        try {
            Object value;
            for (Field field : fields) {
                value = field.get(this);
                DontClearField dontClearField = field.getAnnotation(DontClearField.class);
                if (dontClearField == null) {
                    field.set(this, null);
                } else if (dontClearField.reset()) {
                    if (value instanceof Map<?, ?> map) {
                        map.clear();
                    } else if (value instanceof Collection<?> collection) {
                        collection.clear();
                    } else if (value instanceof AtomicBoolean ab) {
                        ab.set(false);
                    } else if (value instanceof AtomicReference<?> ap) {
                        ap.set(null);
                    }
                }
            }
        } catch (Throwable t) {
            if (BuildConfig.DEBUG) {    // 正式发布版无需关注这里
                getLogger().error("AppInfo清理失败", t);
            }
        }
    }

    /**
     * 该注解用以标识那些不需要被清理的字段
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface DontClearField {
        // 该字段需要被重置
        boolean reset() default false;
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
    @DontClearField
    private final ReentrantLock lock = new ReentrantLock(true);

    @NonNull
    @Override
    public Lock getLock() {
        return lock;
    }
}