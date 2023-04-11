package com.venus.backgroundopt.hook.entity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.venus.backgroundopt.entity.ApplicationIdentity;
import com.venus.backgroundopt.interfaces.ILogger;
import com.venus.backgroundopt.server.ProcessList;
import com.venus.backgroundopt.server.ProcessRecord;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * app信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class AppInfo implements ILogger {
    private int uid;
    private String packageName;
    private int userId;
    private ProcessRecord mProcessRecord;   // 主进程记录
    private int mPid;   // 主进程的pid
    private int mainProcCurAdj = ProcessList.IMPOSSIBLE_ADJ;    // 主进程当前adj
    private int processCount;   // 当前app所运行的进程的数量
    private int appSwitchEvent; // app切换事件
    private Boolean isMultiProcessApp = null;  // 是否是多进程app
    private List<Integer> subProcessPids;   // 子进程pid记录表

    public AppInfo(int userId, String packageName) {
        this.userId = userId;
        this.packageName = packageName;
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
        return mainProcCurAdj;
    }

    public void setMainProcCurAdj(int mainProcCurAdj) {
        this.mainProcCurAdj = mainProcCurAdj;
    }


    public void setMultiProcessApp(Collection<?> collection) {
        isMultiProcessApp = collection.size() > 1;
        getLogger().debug(packageName + "注册的进程数: " + collection.size());
        getLogger().debug(packageName + "注册的进程为: " + collection);
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
        return userId == appInfo.userId && Objects.equals(packageName, appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, userId);
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
//        return this.mProcessRecord.getPid();
        return this.mPid;
    }

    public void setmPid(int mPid) {
        this.mPid = mPid;
    }

    public int getAppSwitchEvent() {
        return appSwitchEvent;
    }

    public void setAppSwitchEvent(int appSwitchEvent) {
        this.appSwitchEvent = appSwitchEvent;
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
        getLogger().debug("mPid = " + mProcessRecord.getPid());
    }
}
