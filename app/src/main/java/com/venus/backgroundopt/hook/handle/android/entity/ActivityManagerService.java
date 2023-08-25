package com.venus.backgroundopt.hook.handle.android.entity;

import static com.venus.backgroundopt.entity.RunningInfo.NormalAppResult;

import android.content.Context;
import android.content.pm.PackageManager;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.log.ILogger;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class ActivityManagerService implements ILogger {
    public final static int MAIN_USER = 0;
    // 用户安装的app的uid大于10000
    public final static int USER_APP_UID_START_NUM = 10000;
    /* *************************************************************************
     *                                                                         *
     * 系统activityManagerService                                               *
     *                                                                         *
     **************************************************************************/
    private final Object activityManagerService;

    public Object getActivityManagerService() {
        return activityManagerService;
    }

    public ActivityManagerService(Object activityManagerService, ClassLoader classLoader) {
        this.activityManagerService = activityManagerService;
        this.processList = new ProcessList(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList),
                this);
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldConstants.mContext);
        this.oomAdjuster = new OomAdjuster(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mOomAdjuster), classLoader);
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
        return process == null ? null : new ProcessRecord(process);
    }

    private final Object mProcLock;

    public Object getmProcLock() {
        return mProcLock;
    }

    public PackageManager getPackageManager() {
        return this.context.getPackageManager();
    }

    private final NormalAppResult notNormalAppResult = new NormalAppResult().setNormalApp(false);

    public NormalAppResult isImportantSystemApp(int userId, String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(userId, packageName);

        // 安卓源码ActivityManagerService.java判断方式:
//            final boolean isSystemApp = process == null ||
//                                      (process.info.flags & (ApplicationInfo.FLAG_SYSTEM |
//                                                             ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
//            final boolean isSystemApp = (
//                    applicationInfo.flags & (
//                            ApplicationInfo.FLAG_SYSTEM |
//                                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        /*
            ① app的uid/100000（10000还是100000）计算为userid填到u0处
            ② app的uid减去10000填到axx处
            例如某个app的uid是10022，则计算出来ps打印得到uid字串就是u0_a22
         */
        //  普通应用程序的UID 都是从 10000开始的
        int uid = Integer.MIN_VALUE;
        if (applicationInfo == null || (uid = applicationInfo.uid) < USER_APP_UID_START_NUM) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("applicationInfo == null ?" + (applicationInfo == null) + ", applicationInfo.uid < USER_APP_UID_START_NUM ? " + (applicationInfo != null && applicationInfo.uid < USER_APP_UID_START_NUM));
            }

            if (uid != Integer.MIN_VALUE) {
                NormalAppResult.normalAppUidMap.put(uid, notNormalAppResult);
            }

            return notNormalAppResult;
        } else {
            if (BuildConfig.DEBUG) {
                getLogger().debug("当前获取的applicationInfo的信息: userId: " + userId + ", packageName: " + packageName + ", uid: " + applicationInfo.uid);
            }

            NormalAppResult normalAppResult = new NormalAppResult();
            normalAppResult.setNormalApp(true);
            normalAppResult.setApplicationInfo(applicationInfo);

            NormalAppResult.normalAppUidMap.put(uid, normalAppResult);

            return normalAppResult;
        }
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
            android.content.pm.ApplicationInfo applicationInfoAsUser =
                    (android.content.pm.ApplicationInfo) XposedHelpers.callMethod(
                            packageManager, MethodConstants.getApplicationInfoAsUser,
                            packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, userId);
            if (applicationInfoAsUser != null) {
                return new ApplicationInfo(applicationInfoAsUser);
            }
        } catch (Throwable throwable) {
            getLogger().warn("获取应用信息失败: [userId = " + userId + ", packageName = " + packageName, throwable);
        }
        return null;
    }

    /**
     * 获取app的uid
     * 注意: 如果是多开app, 通过此方法获取得到的uid仍然为主用户下的uid
     *
     * @param userId      用户id
     * @param packageName 包名
     * @return 指定的app的uid
     */
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
        return androProcessRecord == null ? null : new ProcessRecord(androProcessRecord);
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

        return processRecord == null ? null : new ProcessRecord(processRecord);
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

        return process == null ? null : new ProcessRecord(process);
    }

    public ProcessRecord findMProcessRecord(String packageName, int uid) {
        return findMProcessRecord(1, packageName, uid);
    }

    /**
     * 查找主进程
     *
     * @param depth       查找深度
     * @param packageName 包名
     * @param uid         进程所属uid
     * @return 进程记录
     */
    private ProcessRecord findMProcessRecord(int depth, String packageName, int uid) {
        // 根据进程名查找, 速度最快
        ProcessRecord mProcessRecord = getProcessRecordLocked(packageName, uid);
        if (mProcessRecord == null) {
            if (depth <= 2) {
                mProcessRecord = findMProcessRecord(++depth, packageName, uid);
            } else {    // 保底措施, 遍历进程列表查找进程。效率不够高
//                Object uidRecord = XposedHelpers.newInstance(
//                        UidRecord.getUidRecordClass(classLoader),
//                        uid,
//                        activityManagerService.getActivityManagerService());
//                Object process = XposedHelpers.callMethod(uidRecord, MethodConstants.getProcessInPackage, packageName);
//                if (process != null) {
//                    mProcessRecord = new ProcessRecord(process);
//                } else {
                mProcessRecord = processList.getMProcessRecordLocked(packageName);
//                }
            }
        }

        return mProcessRecord;
    }

    /**
     * 根据{@link AppInfo}找到其主进程
     *
     * @param appInfo app信息
     * @return 主进程的记录
     */
    public ProcessRecord findMProcessRecord(AppInfo appInfo) {
        return findMProcessRecord(appInfo.getPackageName(), appInfo.getUid());
    }
}
