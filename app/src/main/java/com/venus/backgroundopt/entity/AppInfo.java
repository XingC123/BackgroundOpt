package com.venus.backgroundopt.entity;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.annotation.UsageComment;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt;
import com.venus.backgroundopt.utils.log.ILogger;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * app信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class AppInfo implements ILogger {
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

    public AppInfo(int userId, String packageName, RunningInfo runningInfo) {
        this.userId = userId;
        this.packageName = packageName;
        this.runningInfo = runningInfo;
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息                                                               *
     *                                                                         *
     **************************************************************************/
    @SuppressWarnings("all")    // 别显示未final啦, 烦死辣
    private RunningInfo runningInfo;
    private volatile ProcessRecordKt mProcessRecord;   // 主进程记录

    private final AtomicInteger appSwitchEvent = new AtomicInteger(Integer.MIN_VALUE); // app切换事件

    private final AtomicBoolean switchEventHandled = new AtomicBoolean(false);    // 切换事件处理完毕

    public boolean isSwitchEventHandled() {
        return switchEventHandled.get();
    }

    public void setSwitchEventHandled(boolean switchEventHandled) {
        this.switchEventHandled.set(switchEventHandled);
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息(简单)                                                         *
     *                                                                         *
     **************************************************************************/

    // 当前app在本模块内的内存分组
    private volatile AppGroupEnum appGroupEnum = AppGroupEnum.NONE;
    private final Object appGroupSetLock = new Object();

    public void setAppGroupEnum(AppGroupEnum appGroupEnum) {
        synchronized (appGroupSetLock) {
            this.appGroupEnum = appGroupEnum;
            // 添加 切后台时所创建的所有进程(当app进入后台后, 新创建的子进程会自动被添加到待压缩列表中)
            if (!Objects.equals(runningInfo.getActiveLaunchPackageName(), packageName)) {   // 不是桌面进程
                switch (appGroupEnum) {
                    case IDLE -> runningInfo.getProcessManager().addCompactProcess(getProcesses());
                    case ACTIVE ->  /* 若为第一次打开app, 此时也会执行这里。此时无需考虑是否能移除, 因为还没开始添加(至少也要在app第一次进入后台才会开始添加) */
                            runningInfo.getProcessManager().cancelCompactProcess(getProcesses());
                }
            }
        }
    }

    public AppGroupEnum getAppGroupEnum() {
        return appGroupEnum;
    }

    /**
     * 进程信息映射<pid, ProcessInfo>
     * 没有设置为 final, 因为在{@link AppInfo#clearAppInfo()}中需要反射来置空
     */
    @SuppressWarnings("all")    // 别显示未final啦, 烦死辣
    private Map<Integer, ProcessRecordKt> processRecordMap = new ConcurrentHashMap<>();

    public ProcessRecordKt addProcess(int pid, int oomAdjScore) {
        ProcessRecordKt processRecord = runningInfo.getActivityManagerService().getProcessRecord(pid);

        processRecord.setOomAdjScoreToAtomicInteger(oomAdjScore);
        addProcess(processRecord);

        return processRecord;
    }

    public ProcessRecordKt addProcess(@NonNull ProcessRecordKt processRecord) {
        return processRecordMap.computeIfAbsent(processRecord.getPid(), k -> {
            ProcessRecordKt.addCompactProcess(runningInfo, this, processRecord);
            if (processRecord.getMainProcess()) {
                setmProcessRecord(processRecord);
            }
            return processRecord;
        });
    }

    @Nullable
    public ProcessRecordKt getProcess(int pid) {
        return processRecordMap.get(pid);
    }

    public void modifyProcessRecord(int pid, int oomAdjScore) {
        processRecordMap.computeIfPresent(pid, (key, processRecord) -> {
            processRecord.setOomAdjScoreToAtomicInteger(oomAdjScore);
            return processRecord;
        });
    }

    public boolean isRecordedProcess(int pid) {
        return processRecordMap.containsKey(pid);
    }

    public ProcessRecordKt removeProcess(int pid) {
        return processRecordMap.remove(pid);
    }

    public Set<Integer> getProcessPids() {
        return processRecordMap.keySet();
    }

    public Collection<ProcessRecordKt> getProcesses() {
        return processRecordMap.values();
    }

    @UsageComment("尽量避免直接操作map, 推荐用提供好的方法")
    public Map<Integer, ProcessRecordKt> getProcessRecordMap() {
        return processRecordMap;
    }

    /**
     * 安卓ProcessRecord的pid并不是在构造方法中赋值, 而是使用了setPid(int pid)的方式
     * 因而可能导致 {@link com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook}.handleAppSwitch 获取的主进程pid=0
     */
    public ProcessRecordKt correctMainProcess(int correctPid) {
        try {
            return processRecordMap.computeIfPresent(correctPid, ((pid, redundantProcessRecord) -> {
                ProcessRecordKt process = removeProcess(getmPid());
                process.setPid(correctPid);
                process.setRedundantProcessRecord(redundantProcessRecord);
                return process;
            }));
        } catch (Exception e) {
            return null;
        }
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

    @Nullable
    public ProcessRecordKt getmProcess() {
        return mProcessRecord;
    }

    /**
     * 设置主进程信息
     */
    public void setMProcessAndAdd(@NonNull ProcessRecordKt processRecord) {
        // 保存主进程
        setmProcessRecord(processRecord);
        addProcess(processRecord);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return uid == appInfo.uid && userId == appInfo.userId && Objects.equals(packageName, appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, packageName, userId);
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
                .filter(field -> !(field.getType().isPrimitive() || field.getType() == Field[].class))
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
                field.set(this, null);
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

    public ProcessRecordKt getmProcessRecord() {
        return mProcessRecord;
    }

    public void setmProcessRecord(ProcessRecordKt mProcessRecord) {
        this.mProcessRecord = mProcessRecord;

        if (BuildConfig.DEBUG) {
            getLogger().debug("设置ProcessRecord(" + mProcessRecord.getPackageName() + "-" + mProcessRecord.getPid() + ")");
        }
    }
}
