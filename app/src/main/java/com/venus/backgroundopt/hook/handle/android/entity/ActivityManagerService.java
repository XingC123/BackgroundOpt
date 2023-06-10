package com.venus.backgroundopt.hook.handle.android.entity;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.interfaces.ILogger;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class ActivityManagerService implements ILogger {
    public final static int MAIN_USER = 0;
    /* *************************************************************************
     *                                                                         *
     * 系统activityManagerService                                               *
     *                                                                         *
     **************************************************************************/
    private final Object activityManagerService;

    public Object getActivityManagerService() {
        return activityManagerService;
    }

    public ActivityManagerService(Object activityManagerService) {
        this.activityManagerService = activityManagerService;
        this.processList = new ProcessList(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList),
                this);
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldConstants.mContext);
        this.oomAdjuster = new OomAdjuster(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mOomAdjuster));
        this.mPidsSelfLocked = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mPidsSelfLocked);
        this.mProcLock = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcLock);
    }

    private final OomAdjuster oomAdjuster;

    public OomAdjuster getOomAdjuster() {
        return oomAdjuster;
    }

    private final ProcessList processList;

    public ProcessList getProcessList() {
        return processList;
    }

    private final Context context;

    public Context getContext() {
        return context;
    }

    private final Object mPidsSelfLocked; // PidMap

    public ProcessRecord getProcessRecord(int pid) {
        Object process = XposedHelpers.callMethod(mPidsSelfLocked, MethodConstants.get, pid);
        return new ProcessRecord(process);
    }

    private Object mProcLock;

    public Object getmProcLock() {
        return mProcLock;
    }

    public PackageManager getPackageManager() {
        return this.context.getPackageManager();
    }

    public RunningInfo.NormalAppResult isImportantSystemApp(String packageName) {
        RunningInfo.NormalAppResult normalAppResult = new RunningInfo.NormalAppResult();
        ApplicationInfo applicationInfo = getApplicationInfo(MAIN_USER, packageName);

        if (applicationInfo == null) {
            normalAppResult.setNormalApp(false);
        } else {
            boolean importantSystemApp = applicationInfo.uid < 10000;
            if (!importantSystemApp) {
                normalAppResult.setNormalApp(true);
                normalAppResult.setApplicationInfo(applicationInfo);
            }
        }

        return normalAppResult;
    }

    public ApplicationInfo getApplicationInfo(AppInfo appInfo) {
        return getApplicationInfo(appInfo.getUserId(), appInfo.getPackageName());
    }

    /**
     * 获取app信息
     *
     * @param userId      用户id
     * @param packageName 包名
     * @return app信息
     */
    public ApplicationInfo getApplicationInfo(int userId, String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            Object applicationInfoAsUser =
                    XposedHelpers.callMethod(
                            packageManager, MethodConstants.getApplicationInfoAsUser,
                            packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, userId);
            if (applicationInfoAsUser != null) {
                return (ApplicationInfo) applicationInfoAsUser;
            }
        } catch (Throwable throwable) {
            getLogger().warn("获取应用信息失败: [userId = " + userId + ", packageName = " + packageName, throwable);
        }
        return null;
    }

    public int getAppUID(int userId, String packageName) {
        return getApplicationInfo(userId, packageName).uid;
    }

    public boolean isForegroundApp(AppInfo appInfo) {
        String packageName = appInfo.getPackageName();
        int userId = appInfo.getUserId();
        ApplicationInfo applicationInfo = getApplicationInfo(userId, packageName);
        if (applicationInfo == null) {
            return true;
        }
        int uid = applicationInfo.uid;
        Class<?> clazz = activityManagerService.getClass();
        while (clazz != null && !clazz.getName().equals(Object.class.getName()) && !clazz.getName().equals(ClassConstants.ActivityManagerService)) {
            clazz = clazz.getSuperclass();
        }
        if (clazz == null || !clazz.getName().equals(ClassConstants.ActivityManagerService)) {
            getLogger().error("super activityManagerService is not found");
            return true;
        }
        try {
            return (boolean) XposedHelpers.findMethodBestMatch(clazz, MethodConstants.isAppForeground, uid).invoke(activityManagerService, uid);
        } catch (IllegalAccessException | InvocationTargetException e) {
            getLogger().error("call isAppForeground method error");
        }
        return true;
    }

    public boolean isAppForeground(int uid) {
        return (boolean) XposedHelpers.callMethod(
                activityManagerService,
                MethodConstants.isAppForeground,
                uid
        );
    }

    public ProcessRecord getTopApp() {
        Object androProcessRecord = XposedHelpers.callMethod(getActivityManagerService(), MethodConstants.getTopApp);
        return new ProcessRecord(androProcessRecord);
    }

    /**
     * 查找进程
     *
     * @param process  pid字符串
     * @param userId   用户id
     * @param callName 调用方名字
     */
    public ProcessRecord findProcessLOSP(String process, int userId, String callName) {
        Object processRecord = XposedHelpers.callMethod(
                activityManagerService,
                MethodConstants.findProcessLOSP,
                process,
                userId,
                callName
        );

        return new ProcessRecord(processRecord);
    }

    public ProcessRecord findProcessLOSP(String process, int userId) {
        return findProcessLOSP(process, userId, "setProcessMemoryTrimLevel");
    }

    /**
     * <pre>
     *     ...
     *     final ProcessRecord app = findProcessLOSP(process, userId, "setProcessMemoryTrimLevel");
     *     ...
     * </pre>
     * 详见: {@link ClassConstants#ActivityManagerService#setProcessMemoryTrimLevel(String, int, int)}
     *
     * @param process pid字符串
     * @param userId  用户id
     * @param level   等级
     */
    public boolean setProcessMemoryTrimLevel(String process, int userId, int level) {
        return false;
    }

    /**
     * 根据进程名和uid获取ProcessRecord
     *
     * @param processName 进程名
     * @param uid         uid
     * @return 封装后的ProcessRecord
     */
    public ProcessRecord getProcessRecordLocked(String processName, int uid) {
        Object process = XposedHelpers.callMethod(
                activityManagerService,
                MethodConstants.getProcessRecordLocked,
                processName,
                uid
        );

        if (process == null) {
            return null;
        }
        return new ProcessRecord(process);
    }

    /**
     * 根据{@link AppInfo}找到其主进程
     *
     * @param appInfo app信息
     * @return 主进程的记录
     */
    public ProcessRecord findMProcessRecord(AppInfo appInfo) {
        ProcessRecord mProcessRecord = getProcessRecordLocked(appInfo.getPackageName(), appInfo.getUid());

        if (mProcessRecord == null) {   // 目前已知, 双开的app必走这里
//            Object uidRecord = XposedHelpers.newInstance(
//                    UidRecord.getUidRecordClass(classLoader),
//                    uid,
//                    activityManagerService.getActivityManagerService());
//            Object process = XposedHelpers.callMethod(uidRecord, MethodConstants.getProcessInPackage, packageName);
//            if (process != null) {
//                mProcessRecord = new ProcessRecord(process);
//            } else {
            mProcessRecord = processList.getMProcessRecordLockedWhenThrowException(appInfo);
//            }
        }

        return mProcessRecord;
    }
}
