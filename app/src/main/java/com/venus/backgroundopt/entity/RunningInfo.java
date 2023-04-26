package com.venus.backgroundopt.entity;

import com.venus.backgroundopt.hook.entity.AppInfo;
import com.venus.backgroundopt.interfaces.ILogger;
import com.venus.backgroundopt.server.ActivityManagerService;
import com.venus.backgroundopt.server.ProcessList;
import com.venus.backgroundopt.server.ProcessRecord;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class RunningInfo implements ILogger {
    /* *************************************************************************
     *                                                                         *
     * 封装的activityManagerService                                             *
     *                                                                         *
     **************************************************************************/
    private ActivityManagerService activityManagerService;

    public ActivityManagerService getActivityManagerService() {
        return activityManagerService;
    }

    public void setActivityManagerService(ActivityManagerService activityManagerService) {
        this.activityManagerService = activityManagerService;
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的进程                                                                *
     *                                                                         *
     **************************************************************************/
    public AppInfo lastAppInfo;
    /**
     * 非系统重要进程记录
     * 包名, uid
     */
    private final Map<String, Integer> normalApps = new ConcurrentHashMap<>();
    private final static Object checkNormalAppLockObj = new Object();

    public boolean isNormalApp(AppInfo appInfo, int userID) {
        boolean isNormalApp = normalApps.containsKey(appInfo.getPackageName());
        if (isNormalApp)
            return true;
        else {
            synchronized (checkNormalAppLockObj) {
                isNormalApp = normalApps.containsKey(appInfo.getPackageName());
                if (!isNormalApp) {
                    isNormalApp = !isImportantSystemApp(appInfo.getPackageName());
                    if (isNormalApp)
                        markNormalApp(appInfo, userID);
                }
            }

            return isNormalApp;
        }
    }

    private void markNormalApp(AppInfo appInfo, int userID) {
        normalApps.put(appInfo.getPackageName(), getActivityManagerService().getAppUID(userID, appInfo.getPackageName()));
    }

    public int getNormalAppUid(AppInfo appInfo, int userID) {
        return Objects.requireNonNullElseGet(
                normalApps.get(appInfo.getPackageName()),
                () -> getActivityManagerService().getAppUID(userID, appInfo.getPackageName()));
    }

    /**
     * 只有非重要系统进程才能放置于此
     * uid, AppInfo
     */
    private final Map<Integer, AppInfo> runningApps = new ConcurrentHashMap<>();
    private final Collection<AppInfo> runningAppsInfo = runningApps.values();
    /**
     * 子进程的 pid-adj 映射
     * pid, adj
     */
    private final Map<Integer, Integer> subProcessAdjMap = new ConcurrentHashMap<>();

    public void setSubProcessAdj(int pid, int adj) {
        subProcessAdjMap.put(pid, adj);
    }

    public int getSubProcessAdj(int pid) {
        return Objects.requireNonNullElse(subProcessAdjMap.get(pid), ProcessList.IMPOSSIBLE_ADJ);
    }

    public void removeSubProcessPid(int pid, AppInfo appInfo) {
        subProcessAdjMap.remove(pid);
        appInfo.removeSubProcessPid(pid);
    }

    public boolean isSubProcessRunning(int pid) {
        return subProcessAdjMap.containsKey(pid);
    }

    /**
     * 运行中的重要系统进程(pid, adj)
     */
    private final Map<Integer, Integer> runningImportantSystemApps = new ConcurrentHashMap<>();

    public void setImportantSysAppAdj(int pid, int adj) {
        runningImportantSystemApps.put(pid, adj);
    }

    public int getImportantSysAppAdj(int pid) {
        return Objects.requireNonNullElse(runningImportantSystemApps.get(pid), ProcessList.IMPOSSIBLE_ADJ);
    }

    public void removeImportantSysAppPid(int pid) {
        runningImportantSystemApps.remove(pid);
    }

    public boolean isImportantSysAppPidRunning(int pid) {
        return runningImportantSystemApps.containsKey(pid);
    }

    /**
     * 根据uid判断是否是系统重要app
     *
     * @param uid uid
     * @return 是 => true
     */
    public boolean isImportantSystemApp(int uid) {
        return runningApps.get(uid) == null;
    }

    /**
     * 根据传入的{@link AppInfo}判断是否是系统重要app
     *
     * @param appInfo app信息
     * @return 是 => true
     */
    public boolean isImportantSystemApp(AppInfo appInfo) {
        return isImportantSystemApp(appInfo.getPackageName());
    }

    /**
     * 根据包名判断是否是系统重要app
     *
     * @param packageName 包名
     * @return 是 => true
     */
    public boolean isImportantSystemApp(String packageName) {
        return getActivityManagerService().isImportantSystemApp(packageName);
    }

    /**
     * 根据uid获取正在运行的列表中的app信息
     *
     * @param uid 要查询的uid
     * @return 查询到的app信息
     */
    public AppInfo getRunningAppInfo(int uid) {
        return runningApps.get(uid);
    }

    /**
     * 根据传入的{@link AppInfo}判断是否在运行列表
     *
     * @param appInfo app信息
     * @return 运行 => true
     */
    public boolean isAppRunning(AppInfo appInfo) {
        return runningAppsInfo.contains(appInfo);
    }

    /**
     * 添加app信息到运行列表
     *
     * @param appInfo app信息
     */
    public void addRunningApp(AppInfo appInfo) {
        // 找到主进程进行必要设置
        ProcessRecord mProcessRecord = getActivityManagerService().getProcessList().getMProcessRecord(appInfo);
        if (mProcessRecord != null) {
            // 设置主进程的最大adj(保活)
            mProcessRecord.setDefaultMaxAdj();
            // 将主进程pid保存
            appInfo.setmPid(mProcessRecord.getPid());
            debugLog(isDebugMode() &&
                    getLogger().debug(appInfo.getPackageName() + " 的mpid = " + appInfo.getmPid()));
        }
        // 添加到运行列表
        runningApps.put(appInfo.getUid(), appInfo);
    }

    /**
     * 根据{@link AppInfo}找到其主进程
     *
     * @param appInfo app信息
     * @return 主进程的记录
     */
    public ProcessRecord getMProcessRecord(AppInfo appInfo) {
        return getActivityManagerService().getProcessList().getMProcessRecord(appInfo);
//        return appInfo.getmProcessRecord();
    }

    /**
     * 设置主进程的最大Adj
     *
     * @param appInfo app信息
     */
    public void setMProcessMaxAdj(AppInfo appInfo) {
        ProcessRecord mProcessRecord = getMProcessRecord(appInfo);
        if (mProcessRecord != null) {
            // 设置主进程的最大adj(保活)
            mProcessRecord.setDefaultMaxAdj();
            // 将主进程记录器保存
            appInfo.setmPid(mProcessRecord.getPid());
        }
    }

    /**
     * 根据{@link AppInfo}从运行列表中移除
     *
     * @param appInfo app信息
     */
    public void removeRunningApp(AppInfo appInfo) {
        runningApps.remove(appInfo.getUid());
        appInfo.getSubProcessPids().parallelStream()
                .forEach(subProcessAdjMap::remove);
        appInfo.clearSubProcessPids();
    }
}
