package com.venus.backgroundopt.entity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.interfaces.ILogger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private int uid;
    private String packageName;
    private int userId;

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
    private final RunningInfo runningInfo;
    private volatile ProcessRecord mProcessRecord;   // 主进程记录

    private volatile ProcessInfo mProcessInfo;   // 主进程信息

    private int processCount;   // 当前app所运行的进程的数量
    private volatile int appSwitchEvent = Integer.MIN_VALUE; // app切换事件
    private Boolean isMultiProcessApp = null;  // 是否是多进程app
    private List<Integer> subProcessPids;   // 子进程pid记录表
    /**
     * app是否在前台
     */
    private boolean appForeground;

    public boolean isAppForeground() {
        return appForeground;
    }

    public void setAppForeground(boolean appForeground) {
        this.appForeground = appForeground;
    }

    private volatile boolean switchEventHandled = false;

    public boolean isSwitchEventHandled() {
        return switchEventHandled;
    }

    public void setSwitchEventHandled(boolean switchEventHandled) {
        this.switchEventHandled = switchEventHandled;
    }

    /**
     * app所有进程的信息
     */
    private Map<Integer, ProcessRecord> processRecordMap;

    public Collection<ProcessRecord> getProcessRecordList() {
        return processRecordMap.values();
    }

    /**
     * 将所给列表转换为进程信息映射
     */
    public void setProcessRecordMap(List<ProcessRecord> processRecordList) {
        if (this.processRecordMap == null) {
            this.processRecordMap = new ConcurrentHashMap<>();
        }

        processRecordList.parallelStream()
                .forEach(processRecord -> processRecordMap.put(processRecord.getPid(), processRecord));
    }

    /**
     * 添加进程
     */
    public void addProcessRecord(ProcessRecord processRecord) {
        if (this.processRecordMap == null) {
            this.processRecordMap = new ConcurrentHashMap<>();
        }
        this.processRecordMap.put(processRecord.getPid(), processRecord);
    }

    /**
     * 修改进程oom得分
     *
     * @param pid         进程pid
     * @param oomAdjScore oom得分
     */
    public void modifyProcessRecordedOomAdjScore(int pid, int oomAdjScore) {
        ProcessRecord processRecord;
        if (processRecordMap.containsKey(pid)) {
            processRecord = processRecordMap.get(pid);
        } else {
            processRecord = runningInfo.getTargetProcessRecord(pid);
        }

        if (processRecord == null) {
            return;
        }

        processRecord.setOomAdjScore(oomAdjScore);
        if (oomAdjScore > ProcessList.HOME_APP_ADJ) {
            runningInfo.getProcessManager().compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL);
        }
    }

    /**
     * 移除进程
     *
     * @param pid 进程pid
     */
    public ProcessRecord removeProcessRecord(int pid) {
        ProcessRecord processRecord = null;
        if (processRecordMap.containsKey(pid)) {
            processRecord = processRecordMap.get(pid);
            processRecordMap.remove(pid);
        }

        return processRecord;
    }

    /* *************************************************************************
     *                                                                         *
     * app进程信息(简单)                                                         *
     *                                                                         *
     **************************************************************************/
    // <pid, ProcessInfo>
    private final Map<Integer, ProcessInfo> processInfoMap = new ConcurrentHashMap<>();

    public void addProcessInfo(int pid, int oomAdjScore) {
        processInfoMap.put(pid, new ProcessInfo(uid, pid, oomAdjScore));
    }

    public void addProcessInfo(ProcessInfo processInfo) {
        processInfoMap.put(processInfo.getPid(), processInfo);
    }

    public ProcessInfo getProcessInfo(int pid) {
        return processInfoMap.get(pid);
    }

    public void modifyProcessInfoAndAddIfNull(int pid, int oomAdjScore) {
        if (processInfoMap.containsKey(pid)) {
            ProcessInfo processInfo = processInfoMap.get(pid);
            processInfo.setOomAdjScore(oomAdjScore);
        } else {
            addProcessInfo(pid, oomAdjScore);
        }

        // 当进程实际oomAdjScore大于指定等级。则进行一次压缩
        if (appSwitchEvent == ActivityManagerServiceHook.ACTIVITY_PAUSED && oomAdjScore > ProcessList.SERVICE_B_ADJ
                && !Objects.equals(packageName, runningInfo.getActiveLaunchPackageName())) {
            runningInfo.getProcessManager().compactApp(pid, CachedAppOptimizer.COMPACT_ACTION_FULL);

            if (BuildConfig.DEBUG) {
                getLogger().debug("compactApp: " + packageName + "." + pid + " ->>> " + CachedAppOptimizer.COMPACT_ACTION_FULL);
            }
        } /*else {
            if (BuildConfig.DEBUG) {
                getLogger().debug(packageName + " 不执行压缩: appSwitchEvent: " + appSwitchEvent + ", oomAdjScore: " + oomAdjScore + ", 默认桌面: " + runningInfo.getActiveLaunchPackageName());
            }
        }*/
    }

    public boolean isRecordedProcessInfo(int pid) {
        return processInfoMap.containsKey(pid);
    }

    public void removeProcessInfo(int pid) {
        processInfoMap.remove(pid);
    }

    /**
     * 若子进程pid列表为空, 则初始化
     */
    private void initSubProcessPidListIFNull() {
        if (subProcessPids == null) {
            subProcessPids = new CopyOnWriteArrayList<>();
        }
    }

    /**
     * 添加子进程pid
     *
     * @param pid 子进程pid
     */
    public void addSubProcessPid(int pid) {
        initSubProcessPidListIFNull();
        subProcessPids.add(pid);
    }

    /**
     * 子进程是否正在运行
     *
     * @param pid 子进程pid
     * @return 运行 -> true
     */
    public boolean isSubProcessRunning(int pid) {
        if (subProcessPids == null) {
            return false;
        } else {
            return subProcessPids.contains(pid);
        }
    }

    /**
     * 移除子进程pid
     *
     * @param pid 子进程pid
     */
    public void removeSubProcessPid(int pid) {
        if (subProcessPids != null && subProcessPids.size() > 0) {
            subProcessPids.remove(pid);
        }
    }

    public List<Integer> getSubProcessPids() {
        initSubProcessPidListIFNull();
        return subProcessPids;
    }

    /**
     * 清空子进程pid记录表
     */
    public void clearSubProcessPids() {
        if (subProcessPids != null) {
            subProcessPids.clear();
            subProcessPids = null;
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
        return this.mProcessInfo.getFixedOomAdjScore();
    }

    public void setmProcessInfo(ProcessRecord processRecord) {
        this.mProcessInfo = new ProcessInfo(processRecord);
        addProcessInfo(mProcessInfo);
    }

    public ProcessInfo getmProcessInfo() {
        return mProcessInfo;
    }

    public void setMultiProcessApp(Collection<?> collection) {
        isMultiProcessApp = collection.size() > 1;

        if (BuildConfig.DEBUG) {
            getLogger().debug(packageName + "注册的进程数: " + collection.size());
            getLogger().debug(packageName + "注册的进程为: " + collection);
        }
    }

    public void setDefaultMaxAdj() {
        if (isMultiProcessApp) {
            this.mProcessRecord.setMaxAdj(ProcessRecord.MULTI_PROC_MAX_ADJ);
        } else {
            this.mProcessRecord.setMaxAdj(ProcessRecord.DEFAULT_MAX_ADJ);
        }
    }

    public void setDefaultMaxAdj(PackageManager packageManager) {
        // 判断是否是多进程app
        HashSet<String> componentInfoSet = new HashSet<>();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_ACTIVITIES |
                            PackageManager.GET_SERVICES |
                            PackageManager.GET_RECEIVERS |
                            PackageManager.GET_PROVIDERS
            );

            if (packageInfo.activities != null) {
                Arrays.stream(packageInfo.activities).forEach(pkgInfo -> componentInfoSet.add(pkgInfo.processName));
            }

            if (packageInfo.services != null) {
                Arrays.stream(packageInfo.services).forEach(pkgInfo -> componentInfoSet.add(pkgInfo.processName));
            }
            if (packageInfo.receivers != null) {
                Arrays.stream(packageInfo.receivers).forEach(pkgInfo -> componentInfoSet.add(pkgInfo.processName));
            }
            if (packageInfo.providers != null) {
                Arrays.stream(packageInfo.providers).forEach(pkgInfo -> componentInfoSet.add(pkgInfo.processName));
            }
            setMultiProcessApp(componentInfoSet);
            setDefaultMaxAdj();
        } catch (PackageManager.NameNotFoundException e) {
            getLogger().warn("查询注册的进程数失败", e);
        }
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
    private volatile boolean appInfoCleaned = false;

    public boolean isAppInfoCleaned() {
        return appInfoCleaned;
    }

    public void clearAppInfo() {
        try {
            mProcessRecord = null;
            mProcessInfo = null;
            if (processRecordMap != null) {
                processRecordMap.clear();
            }

            processInfoMap.clear();

            appInfoCleaned = true;
        } catch (Exception ignore) {
        }
    }

    /* *************************************************************************
     *                                                                         *
     * getter/setter                                                           *
     *                                                                         *
     **************************************************************************/

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
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

    public Boolean getMultiProcessApp() {
        return isMultiProcessApp;
    }

    public void setMultiProcessApp(Boolean multiProcessApp) {
        isMultiProcessApp = multiProcessApp;
    }

    public ProcessRecord getmProcessRecord() {
        return mProcessRecord;
    }

    public void setmProcessRecord(ProcessRecord mProcessRecord) {
        this.mProcessRecord = mProcessRecord;
    }
}
