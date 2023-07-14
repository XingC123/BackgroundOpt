package com.venus.backgroundopt.entity;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private int repairedUid = Integer.MIN_VALUE;
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
    private volatile int appSwitchEvent = Integer.MIN_VALUE; // app切换事件

    private volatile boolean switchEventHandled = false;    // 切换事件处理完毕

    public boolean isSwitchEventHandled() {
        return switchEventHandled;
    }

    public void setSwitchEventHandled(boolean switchEventHandled) {
        this.switchEventHandled = switchEventHandled;
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息(简单)                                                         *
     *                                                                         *
     **************************************************************************/
    // 当前app在本模块内的内存分组
    private final AtomicReference<AppGroupEnum> appGroupEnumAtomicReference = new AtomicReference<>();

    public void setAppGroupEnum(AppGroupEnum appGroupEnum) {
        appGroupEnumAtomicReference.set(appGroupEnum);
    }

    public AppGroupEnum getAppGroupEnum() {
        return appGroupEnumAtomicReference.get();
    }

    /**
     * 进程信息映射<pid, ProcessInfo>
     * 没有设置为 final, 因为在{@link AppInfo#clearAppInfo()}中需要反射来置空
     */
    private Map<Integer, ProcessInfo> processInfoMap = new ConcurrentHashMap<>();

    public ProcessInfo addProcessInfo(int pid, int oomAdjScore) {
        ProcessInfo processInfo = new ProcessInfo(repairedUid, pid, oomAdjScore);
        processInfoMap.put(pid, processInfo);

        return processInfo;
    }

    public void addProcessInfo(ProcessInfo processInfo) {
        // 纠正processInfo的uid
        processInfo.setRepairedUid(repairedUid);
        // 添加
        processInfoMap.put(processInfo.getPid(), processInfo);
    }

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

        ProcessInfo processInfo;
        int oldAdj = Integer.MIN_VALUE;

        if (processInfoMap.containsKey(pid)) {
            processInfo = processInfoMap.get(pid);

            oldAdj = processInfo.getOomAdjScore();
            processInfo.setOomAdjScore(oomAdjScore);
        } else {
            processInfo = addProcessInfo(pid, oomAdjScore);

            runningInfo.getProcessManager().setPidToBackgroundProcessGroup(pid, this);
        }

        // 压缩当前进程
        AppGroupEnum curAppGroupEnum = getAppGroupEnum();
        if (Objects.equals(curAppGroupEnum, AppGroupEnum.IDLE) &&
                !Objects.equals(packageName, runningInfo.getActiveLaunchPackageName())) {
            if (!Objects.equals(curAppGroupEnum, processInfo.getLastAppGroupEnum())) {  // app经历了idle->active->idle
                runningInfo.getProcessManager().compactAppFullNoCheck(processInfo);

                if (BuildConfig.DEBUG) {
                    getLogger().debug("包名: " + packageName + "的pid: " + pid + " >>> 因[所在app内存状态改变]而内存压缩");
                }
            } else // 参照了com.android.server.am.CachedAppOptimizer.void onOomAdjustChanged(int oldAdj, int newAdj, ProcessRecord app)
                if (oldAdj < ProcessList.CACHED_APP_MIN_ADJ
                        && oomAdjScore >= ProcessList.CACHED_APP_MIN_ADJ
                        && oomAdjScore <= ProcessList.CACHED_APP_MAX_ADJ) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (processInfo.isAllowedCompact(currentTimeMillis)) {
                        runningInfo.getProcessManager().compactAppFullNoCheck(processInfo);
                        processInfo.setLastCompactTime(currentTimeMillis);

                        if (BuildConfig.DEBUG) {
                            getLogger().debug("包名: " + packageName + "的pid: " + pid + " >>> 因[oom_score]而内存压缩");
                        }
                    }
                }
        }
        // 更新进程记录的app内存分组
        if (!Objects.equals(curAppGroupEnum, AppGroupEnum.TMP)) {
            processInfo.setLastAppGroupEnum(curAppGroupEnum);
        }
    }

    public boolean isRecordedProcessInfo(int pid) {
        return processInfoMap.containsKey(pid);
    }

    public void removeProcessInfo(int pid) {
        processInfoMap.remove(pid);
    }

    public Set<Integer> getProcessInfoPids() {
        if (processInfoMap == null) {
            return null;
        }
        return processInfoMap.keySet();
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
        this.mProcessInfo = new ProcessInfo(processRecord);
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
        return repairedUid == appInfo.repairedUid && userId == appInfo.userId && Objects.equals(packageName, appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repairedUid, packageName, userId);
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
                    } else if (obj instanceof AtomicReference<?> ap) {
                        ap.set(null);
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
        } else {
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

    public int getRepairedUid() {
        return repairedUid;
    }

    public AppInfo setRepairedUid(int repairedUid) {
        this.repairedUid = repairedUid;

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
        return appSwitchEvent;
    }

    public void setAppSwitchEvent(int appSwitchEvent) {
        this.appSwitchEvent = appSwitchEvent;

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
