package com.venus.backgroundopt.hook.handle.android.entity;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.venus.backgroundopt.entity.AppInfo;
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
        this.processList = new ProcessList(XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList));
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldConstants.mContext);
        this.oomAdjuster = new OomAdjuster(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mOomAdjuster));
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

    public PackageManager getPackageManager() {
        return this.context.getPackageManager();
    }

    public boolean isImportantSystemApp(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(MAIN_USER, packageName);
        if (applicationInfo == null) {
            return true;
        }
        return applicationInfo.uid < 10000;
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
            getLogger().warn(packageName, throwable);
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

    public ProcessRecord getTopApp() {
        Object androProcessRecord = XposedHelpers.callMethod(getActivityManagerService(), MethodConstants.getTopApp);
        return new ProcessRecord(androProcessRecord);
    }

    private ProcessRecord findProcessLOSP(String process, int userId, String callName) {
        Object processRecord = XposedHelpers.callMethod(
                activityManagerService,
                MethodConstants.findProcessLOSP,
                process,
                userId,
                callName
        );

        return new ProcessRecord(processRecord);
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
}
