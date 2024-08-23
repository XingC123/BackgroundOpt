/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.common.util.OsUtils;
import com.venus.backgroundopt.common.util.PackageUtils;
import com.venus.backgroundopt.common.util.log.ILogger;
import com.venus.backgroundopt.xposed.annotation.OriginalMethod;
import com.venus.backgroundopt.xposed.annotation.OriginalObject;
import com.venus.backgroundopt.xposed.annotation.OriginalObjectField;
import com.venus.backgroundopt.xposed.core.RunningInfo;
import com.venus.backgroundopt.xposed.entity.android.android.content.pm.ApplicationInfo;
import com.venus.backgroundopt.xposed.entity.self.AppInfo;
import com.venus.backgroundopt.xposed.entity.self.FindAppResult;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;
import com.venus.backgroundopt.xposed.util.XposedUtilsKt;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#ActivityManagerService}
 *
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
    @OriginalObject(classPath = ClassConstants.ActivityManagerService)
    private final Object activityManagerService;

    @OriginalObject
    public Object getActivityManagerService() {
        return activityManagerService;
    }

    public static Class<?> activityManagerServiceClazz;

    public ActivityManagerService(
            @OriginalObject Object activityManagerService,
            ClassLoader classLoader,
            RunningInfo runningInfo
    ) {
        this.activityManagerService = activityManagerService;
        this.processList = new ProcessList(
                XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcessList),
                this);
        this.context = (Context) XposedHelpers.getObjectField(activityManagerService, FieldConstants.mContext);
        this.oomAdjuster = OomAdjuster.newInstance(XposedHelpers.getObjectField(activityManagerService, FieldConstants.mOomAdjuster));
        this.mPidsSelfLocked = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mPidsSelfLocked);
        if (OsUtils.isSOrHigher) {
            this.mProcLock = XposedHelpers.getObjectField(activityManagerService, FieldConstants.mProcLock);
        } else {
            this.mProcLock = activityManagerService;
        }
        this.activityManagerConstants = new ActivityManagerConstants(XposedUtilsKt.getObjectFieldValue(activityManagerService, FieldConstants.mConstants, null));

        this.runningInfo = runningInfo;

        activityManagerServiceClazz = activityManagerService.getClass();
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

    private final ActivityManagerConstants activityManagerConstants;

    @NonNull
    public ActivityManagerConstants getActivityManagerConstants() {
        return activityManagerConstants;
    }

    @OriginalObjectField(
            objectClassPath = ClassConstants.ActivityManagerService,
            fieldName = FieldConstants.mPidsSelfLocked
    )
    private final Object mPidsSelfLocked; // PidMap

    @OriginalObjectField(
            objectClassPath = ClassConstants.ActivityManagerService,
            fieldName = FieldConstants.mProcLock
    )
    private final Object mProcLock;

    @OriginalObjectField
    public Object getmProcLock() {
        return mProcLock;
    }

    public PackageManager getPackageManager() {
        return this.context.getPackageManager();
    }

    @OriginalObject
    public static Object getAppProfiler() {
        return XposedUtilsKt.getObjectFieldValue(
                RunningInfo.getInstance().getActivityManagerService().activityManagerService,
                FieldConstants.mAppProfiler,
                null
        );
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
        return PackageUtils.isImportantSystemApp(applicationInfo);
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

    public FindAppResult getFindAppResult(int userId, String packageName, @Nullable android.content.pm.ApplicationInfo applicationInfo) {
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

    public static boolean isUnsafeUid(@OriginalObject Object proc) {
        return isUnsafeUid(ProcessRecord.getUID(proc));
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

    @OriginalMethod(classPath = ClassConstants.ActivityManagerService, methodName = MethodConstants.forceStopPackage)
    public void forceStopPackage(String packageName, int userId) {
        XposedUtilsKt.callMethod(activityManagerService, MethodConstants.forceStopPackage, packageName, userId);
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