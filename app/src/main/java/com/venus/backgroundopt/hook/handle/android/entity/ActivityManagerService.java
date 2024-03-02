package com.venus.backgroundopt.hook.handle.android.entity;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.annotation.AndroidObjectField;
import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.FindAppResult;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.PackageUtils;
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
    @AndroidObject(classPath = ClassConstants.ActivityManagerService)
    private final Object activityManagerService;

    @AndroidObject
    public Object getActivityManagerService() {
        return activityManagerService;
    }

    public ActivityManagerService(
            @AndroidObject Object activityManagerService,
            ClassLoader classLoader,
            RunningInfo runningInfo
    ) {
        this.activityManagerService = activityManagerService;
        this.processList = new ProcessList(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList),
                this);
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldConstants.mContext);
        this.oomAdjuster = new OomAdjuster(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mOomAdjuster), classLoader);
        this.mPidsSelfLocked = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mPidsSelfLocked);
        this.mProcLock = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcLock);

        this.runningInfo = runningInfo;
    }

    private final RunningInfo runningInfo;

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

    @AndroidObjectField(
            objectClassPath = ClassConstants.ActivityManagerService,
            fieldName = FieldConstants.mPidsSelfLocked
    )
    private final Object mPidsSelfLocked; // PidMap

    @NonNull
    public ProcessRecordKt getProcessRecord(int pid) {
        return new ProcessRecordKt(
                this,
                XposedHelpers.callMethod(mPidsSelfLocked, MethodConstants.get, pid)
        );
    }

    @AndroidObjectField(
            objectClassPath = ClassConstants.ActivityManagerService,
            fieldName = FieldConstants.mProcLock
    )
    private final Object mProcLock;

    @AndroidObjectField
    public Object getmProcLock() {
        return mProcLock;
    }

    public PackageManager getPackageManager() {
        return this.context.getPackageManager();
    }

    /**
     * 是否是重要的系统app。<br>
     * 安卓源码ActivityManagerService.java判断方式:
     * <pre>
     *     final boolean isSystemApp = process == null ||
     *                                       (process.info.flags & (ApplicationInfo.FLAG_SYSTEM |
     *                                                              ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
     *             final boolean isSystemApp = (
     *                     applicationInfo.flags & (
     *                             ApplicationInfo.FLAG_SYSTEM |
     *                                     ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
     * </pre>
     * <p>
     * 关于UID: <br>
     * 1. app的uid/100000（10000还是100000）计算为userid填到u0处<br>
     * 2. app的uid减去10000填到axx处<br>
     * 例如某个app的uid是10022，则计算出来ps打印得到uid字串就是u0_a22<br>
     * 普通应用程序的UID 都是从 10000开始的<br>
     *
     * @param applicationInfo 安卓的{@link android.content.pm.ApplicationInfo}实例对象
     * @return 是重要系统app -> true
     */
    public static boolean isImportantSystemApp(android.content.pm.ApplicationInfo applicationInfo) {
        return applicationInfo == null ||
                (applicationInfo.flags &
                        /*(android.content.pm.ApplicationInfo.FLAG_SYSTEM | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                        )*/
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM
                ) != 0;
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

    private final FindAppResult notNormalAppFindAppResult = new FindAppResult();

    public FindAppResult getFindAppResult(int userId, String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(userId, packageName);

        if (applicationInfo == null) {
            return notNormalAppFindAppResult;
        } else {
            FindAppResult findAppResult = new FindAppResult(applicationInfo);
            findAppResult.setHasActivity(isHasActivity(packageName));
            return findAppResult;
        }
    }

    public static boolean isUnsafeUid(int uid) {
        return uid < USER_APP_UID_START_NUM;
    }

    public static boolean isUnsafeUid(@AndroidObject Object proc) {
        return isUnsafeUid(ProcessRecordKt.getUID(proc));
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

    public boolean isHasActivity(String packageName) {
        try {
            return PackageUtils.INSTANCE.isHasActivity(
                    getPackageManager().getPackageInfo(
                            packageName,
                            PackageManager.GET_ACTIVITIES
                    )
            );
        } catch (Throwable throwable) {
            return false;
        }
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

    public ProcessRecordKt getTopApp() {
        Object androProcessRecord = XposedHelpers.callMethod(getActivityManagerService(), MethodConstants.getTopApp);
        return androProcessRecord == null ? null : new ProcessRecordKt(this, androProcessRecord);
    }

    /**
     * 查找进程
     *
     * @param process  pid字符串
     * @param userId   用户id
     * @param callName 调用方名字
     */
    public ProcessRecordKt findProcessLOSP(String process, int userId, String callName) {
        Object processRecord = XposedHelpers.callMethod(
                activityManagerService,
                MethodConstants.findProcessLOSP,
                process,
                userId,
                callName
        );

        return processRecord == null ? null : new ProcessRecordKt(this, processRecord);
    }

    public ProcessRecordKt findProcessLOSP(String process, int userId) {
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
    public ProcessRecordKt getProcessRecordLocked(String processName, int uid) {
        Object process = XposedHelpers.callMethod(
                activityManagerService,
                MethodConstants.getProcessRecordLocked,
                processName,
                uid
        );

        return process == null ? null : new ProcessRecordKt(this, process);
    }

    public ProcessRecordKt findMProcessRecord(String packageName, int uid) {
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
    private ProcessRecordKt findMProcessRecord(int depth, String packageName, int uid) {
        // 根据进程名查找, 速度最快
        ProcessRecordKt mProcessRecord = getProcessRecordLocked(packageName, uid);
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

        // pid纠察
        ProcessRecordKt.correctProcessPid(mProcessRecord);

        return mProcessRecord;
    }

    /**
     * 根据{@link AppInfo}找到其主进程
     *
     * @param appInfo app信息
     * @return 主进程的记录
     */
    public ProcessRecordKt findMProcessRecord(AppInfo appInfo) {
        return findMProcessRecord(appInfo.getPackageName(), appInfo.getUid());
    }
}
