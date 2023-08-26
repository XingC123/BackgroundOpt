package com.venus.backgroundopt.entity;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.utils.log.ILogger;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
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
    private RunningInfo runningInfo;
    private volatile ProcessRecord mProcessRecord;   // 主进程记录

    private volatile ProcessInfo mProcessInfo;   // 主进程信息
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
                    case IDLE ->
                            runningInfo.getProcessManager().addCompactProcessInfo(getProcessInfos());
                    case ACTIVE ->  /* 若为第一次打开app, 此时也会执行这里。此时无需考虑是否能移除, 因为还没开始添加(至少也要在app第一次进入后台才会开始添加) */
                            runningInfo.getProcessManager().cancelCompactProcessInfo(getProcessInfos());
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
    private Map<Integer, ProcessInfo> processInfoMap = new ConcurrentHashMap<>();

    public ProcessInfo addProcessInfo(int pid, int oomAdjScore) {
        ProcessInfo processInfo = processInfoMap.computeIfAbsent(pid,
                key -> ProcessInfo.newInstance(this, runningInfo, uid, pid, oomAdjScore));

        return processInfo;
    }

    public void addProcessInfo(ProcessInfo processInfo) {
        // 纠正processInfo的uid
        processInfo.setUid(uid);
        // 添加
        processInfoMap.put(processInfo.getPid(), processInfo);
    }

    @Nullable
    public ProcessInfo getProcessInfo(int pid) {
        return processInfoMap.get(pid);
    }

    public void modifyProcessInfoAndAddIfNull(int pid, int oomAdjScore) {
//        if (pid == Integer.MIN_VALUE) {
//            if (BuildConfig.DEBUG) {
//                getLogger().warn("pid = " + pid + " 不符合规范, 无法添加至进程列表");
//            }
//            return;
//        }

        if (processInfoMap.containsKey(pid)) {
            ProcessInfo processInfo = processInfoMap.get(pid);
            processInfo.setOomAdjScore(oomAdjScore);
        } else {
            addProcessInfo(pid, oomAdjScore);

//            runningInfo.getProcessManager().setPidToBackgroundProcessGroup(pid, this);
        }
    }

    public boolean isRecordedProcessInfo(int pid) {
        return processInfoMap.containsKey(pid);
    }

    public ProcessInfo removeProcessInfo(int pid) {
        return processInfoMap.remove(pid);
    }

    public Set<Integer> getProcessInfoPids() {
        return processInfoMap.keySet();
    }

    public Collection<ProcessInfo> getProcessInfos() {
        return processInfoMap.values();
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
        return this.mProcessInfo.getFixedOomAdjScore();
    }

    public void setmProcessInfo(ProcessRecord processRecord) {
        this.mProcessInfo = ProcessInfo.newInstance(this, runningInfo, processRecord);
        addProcessInfo(mProcessInfo);
    }

    public ProcessInfo getmProcessInfo() {
        return mProcessInfo;
    }

    /**
     * 设置主进程信息
     */
    public void setMProcessInfoAndMProcessRecord(ProcessRecord processRecord) {
        // 保存进程信息
        setmProcessInfo(processRecord);
        // 保存主进程
        setmProcessRecord(processRecord);
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

    /* *************************************************************************
     *                                                                         *
     * 当前appInfo清理状态                                                       *
     *                                                                         *
     **************************************************************************/
    private static final Field[] fields = AppInfo.class.getDeclaredFields();

    static {
        for (Field field : fields) {
            field.setAccessible(true);
        }
    }

    public void clearAppInfo() {
        try {
            runningInfo = null;
            mProcessRecord = null;
            mProcessInfo = null;

            try {
                Object obj;
                for (Field field : fields) {
                    obj = field.get(this);
                    if (obj instanceof Map<?, ?> map) {
                        map.clear();
                        field.set(this, null);
                    } else if (obj instanceof Collection<?> collection) {
                        collection.clear();
                    } else if (obj instanceof AtomicReference<?> ap) {
                        ap.set(null);
                    } else if (obj instanceof Enum<?>) {
                        field.set(this, null);
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                if (BuildConfig.DEBUG) {
                    getLogger().error("AppInfo的map清理失败", e);
                }
            }
        } catch (Exception ignore) {
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

    public ProcessRecord getmProcessRecord() {
        return mProcessRecord;
    }

    public void setmProcessRecord(ProcessRecord mProcessRecord) {
        this.mProcessRecord = mProcessRecord;

        if (BuildConfig.DEBUG) {
            getLogger().debug("设置ProcessRecord(" + mProcessRecord.getPackageName() + "-" + mProcessRecord.getPid() + ")");
        }
    }
}
