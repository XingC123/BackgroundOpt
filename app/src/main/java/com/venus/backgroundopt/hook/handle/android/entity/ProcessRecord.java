package com.venus.backgroundopt.hook.handle.android.entity;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.PackageUtils;

import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#ProcessRecord}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class ProcessRecord {
    // 默认的最大adj
//    public static final int DEFAULT_MAX_ADJ = ProcessList.FOREGROUND_APP_ADJ;
    public static final int DEFAULT_MAX_ADJ = ProcessList.VISIBLE_APP_ADJ;
    // 多进程app的最大adj
    public static final int MULTI_PROC_MAX_ADJ = ProcessList.PERSISTENT_PROC_ADJ;
    // 默认的主进程要设置的adj
//    public static final int DEFAULT_MAIN_ADJ = ProcessList.NATIVE_ADJ;
//    public static final int DEFAULT_MAIN_ADJ = ProcessList.SERVICE_ADJ;
//    public static final int DEFAULT_MAIN_ADJ = ProcessRecord.DEFAULT_MAX_ADJ;
    public static final int DEFAULT_MAIN_ADJ = ProcessList.FOREGROUND_APP_ADJ;
    // 默认的子进程要设置的adj
    public static final int SUB_PROC_ADJ = ProcessList.PREVIOUS_APP_ADJ;
    /**
     * 安卓的ProcessRecord类
     */
    private static Class<?> ProcessRecordClass;

    public static Class<?> getProcessRecordClass(ClassLoader classLoader) {
        if (ProcessRecordClass == null) {
            ProcessRecordClass = XposedHelpers.findClass(
                    ClassConstants.ProcessRecord,
                    classLoader
            );
        }
        return ProcessRecordClass;
    }

    // 程序的uid
    private final int uid;
    // 该进程对应的pid
    private final int pid;
    // 进程名称
    private final String processName;
    // 进程所属程序的用户id
    private final int userId;
    // 进程所在的程序的信息
    private final ApplicationInfo applicationInfo;
    // 进程所在的程序的包名
    private final String packageName;
    // 进程的oom_score
    private int oomAdjScore;
    // 反射拿到的安卓的processRecord对象
    private Object processRecord;
    // 反射拿到的安卓的processStateRecord对象
    private Object processStateRecord;
    // 自定义的进程最大adj
    private int mMaxAdj;

    public ProcessRecord(Object processRecord) {
        this.processRecord = processRecord;
        this.pid = getPid(processRecord);
//        this.uid = XposedHelpers.getIntField(processRecord, FieldConstants.uid);

        this.userId = getUserId(processRecord);
        this.applicationInfo = (ApplicationInfo) XposedHelpers.getObjectField(processRecord, FieldConstants.info);
        this.packageName = applicationInfo.packageName;
        this.uid = applicationInfo.uid;
        this.processName = getAbsoluteProcessName(packageName, processRecord);
        this.processStateRecord = XposedHelpers.getObjectField(processRecord, FieldConstants.mState);
    }

    /**
     * 获取进程的用户id
     *
     * @param processRecord 安卓的进程记录
     */
    public static int getUserId(Object processRecord) {
        return XposedHelpers.getIntField(processRecord, FieldConstants.userId);
    }

    /**
     * 获取包名
     *
     * @param processRecord 安卓源码中的进程记录器
     * @return 包名
     */
    public static String getPkgName(Object processRecord) {
        return ((ApplicationInfo) XposedHelpers.getObjectField(processRecord, FieldConstants.info)).packageName;
    }

    /**
     * 获取进程名
     *
     * @param processRecord 安卓源码中进程记录器
     * @return 进程名
     */
    public static String getAbsoluteProcessName(Object processRecord) {
        return PackageUtils.absoluteProcessName(
                getPkgName(processRecord),
                (String) XposedHelpers.getObjectField(processRecord, FieldConstants.processName)
        );
    }

    public static String getAbsoluteProcessName(String packageName, Object processRecord) {
        return PackageUtils.absoluteProcessName(packageName, getProcessName(processRecord));
    }

    public String getAbsoluteProcessName() {
        return PackageUtils.absoluteProcessName(packageName, processName);
    }

    /**
     * 获取进程名
     *
     * @param processRecord 安卓的ProcessRecord 对象
     * @return 进程名
     */
    public static String getProcessName(Object processRecord) {
        return (String) XposedHelpers.getObjectField(processRecord, FieldConstants.processName);
    }

    /**
     * 判断进程名是否相同
     *
     * @param expectProcName 期望进程名
     * @param processRecord  要比较的、安卓的ProcessRecord
     * @return 相同 -> true
     */
    public static boolean isProcessAbsoluteNameSame(String expectProcName, Object processRecord) {
//        String packageName = getPkgName(processRecord);
//        return packageName.contains(expectProcName) &&
//                expectProcName.equals(getAbsoluteProcessName(packageName, processRecord));
        return expectProcName.equals(getAbsoluteProcessName(processRecord));
    }

    public static boolean isProcessNameSame(String expectProcName, Object processRecord) {
        return expectProcName.equals(getProcessName(processRecord));
    }

    /**
     * 设置默认的最大adj
     */
    public void setDefaultMaxAdj() {
        setMaxAdj(DEFAULT_MAX_ADJ);
    }

    /**
     * 设置指定的最大adj
     * 注意:
     * <pre>
     *     在Redmi k30p MIUI13 22.7.11 (Android 12)中, 设置小于0的值(未充分测试, 只设置过-800) 且 打开的app是单进程,
     *     会导致在最近任务上划无法杀死app。
     *     在另一台机器Redmi Note5p Nusantara v5.2 official (Android安全更新2022.11.5, Android 13)中无此问题
     * </pre>
     *
     * @param maxAdj 最大adj的值
     */
    public void setMaxAdj(int maxAdj) {
        try {
            XposedHelpers.callMethod(this.processStateRecord, MethodConstants.setMaxAdj, maxAdj);
        } catch (Exception e) {
            XposedHelpers.setIntField(this.processStateRecord, FieldConstants.mMaxAdj, maxAdj);
        } finally {
            this.mMaxAdj = maxAdj;
        }
    }

    /**
     * 获取进程的最大adj
     *
     * @return 进程的最大adj
     */
    public int getMaxAdj() {
        return XposedHelpers.getIntField(this.processStateRecord, FieldConstants.mMaxAdj);
    }

    /**
     * 获取pid
     *
     * @param processRecord 安卓ProcessRecord
     */
    public static int getPid(Object processRecord) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return XposedHelpers.getIntField(processRecord, FieldConstants.mPid);
        } else {
            return XposedHelpers.getIntField(processRecord, FieldConstants.pid);
        }
    }

    /**
     * 设置trim
     *
     * @param level trim级别({@link ComponentCallbacks2})
     * @return 成功设置 -> true
     */
    public boolean scheduleTrimMemory(int level) {
//        XposedHelpers.callMethod(mThread, MethodConstants.scheduleTrimMemory, level);
        Object thread = null;

        try {
            thread = XposedHelpers.callMethod(processRecord, MethodConstants.getThread);
        } catch (Exception ignore) {
        }

        if (thread == null) {
            return false;
        }
        XposedHelpers.callMethod(thread, MethodConstants.scheduleTrimMemory, level);

        return true;
    }

    /**
     * 是否需要调整最大adj
     *
     * @return 若已设置的最大adj!=当前所使用的最大adj => true
     */
    public boolean isNeedAdjustMaxAdj() {
        return getMaxAdj() != this.mMaxAdj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessRecord that = (ProcessRecord) o;
        return uid == that.uid && pid == that.pid && userId == that.userId && Objects.equals(processName, that.processName) && Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, pid, processName, userId, packageName);
    }

    public int getUid() {
        return uid;
    }

    public int getPid() {
        return pid;
    }

    public String getProcessName() {
        return processName;
    }

    public int getUserId() {
        return userId;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public String getPackageName() {
        return packageName;
    }

    public Object getProcessRecord() {
        return processRecord;
    }

    public void setProcessRecord(Object processRecord) {
        this.processRecord = processRecord;
    }

    public Object getProcessStateRecord() {
        return processStateRecord;
    }

    public void setProcessStateRecord(Object processStateRecord) {
        this.processStateRecord = processStateRecord;
    }

    public int getOomAdjScore() {
        return oomAdjScore;
    }

    public void setOomAdjScore(int oomAdjScore) {
        this.oomAdjScore = oomAdjScore;
    }
}
