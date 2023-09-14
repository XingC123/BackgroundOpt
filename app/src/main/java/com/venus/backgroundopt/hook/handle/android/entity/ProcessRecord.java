package com.venus.backgroundopt.hook.handle.android.entity;

import android.content.pm.ApplicationInfo;

import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

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
    public static final int DEFAULT_MAX_ADJ = ProcessList.VISIBLE_APP_ADJ;
    // 默认的主进程要设置的adj
    public static final int DEFAULT_MAIN_ADJ = ProcessList.FOREGROUND_APP_ADJ;
    // 默认的子进程要设置的adj
    public static final int SUB_PROC_ADJ = DEFAULT_MAX_ADJ + 1;

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
    private final int uid;  // uid of process; may be different from 'info' if isolated
    // 该进程对应的pid
    private final int pid;
    // 进程名称
    /*
        com.ktcp.video
        com.ktcp.video:push
        com.ktcp.video:upgrade
        第一个很明显，是主应用的进程，下边带冒号":"的一般都是通过在manifest中声明android:process来指定的一个独立进程。
        这里每一个进程在系统framework中都有一个对应的ProcessRecord数据结构来维护各个进程的状态信息等。

        另外需要注意的是: ProcessRecord.processName获取的是每个独立进程的完整名字，也就是带冒号":"的名字;
        而通过ProcessRecord.info.processName获取的是主应用进程的进程名，也就是不带有冒号":"的名字
        来源: https://blog.csdn.net/weixin_35831256/article/details/117644536
     */
    private final String processName;
    // 进程所属程序的用户id
    private final int userId;
    // 进程所在的程序的包名
    private final String packageName;
    // 反射拿到的安卓的processRecord对象
    private final Object processRecord;
    // 反射拿到的安卓的processStateRecord对象
//    private final Object processStateRecord;
    private final ProcessStateRecord processStateRecord;
    // 当前ProcessRecord已记录的最大adj
    private int recordMaxAdj;

    /**
     * All about the state info of the optimizer when the process is cached.
     */
    private ProcessCachedOptimizerRecord processCachedOptimizerRecord;

    public ProcessCachedOptimizerRecord getProcessCachedOptimizerRecord() {
        if (processCachedOptimizerRecord == null) {
            processCachedOptimizerRecord = new ProcessCachedOptimizerRecord(
                    XposedHelpers.getObjectField(this.processRecord, FieldConstants.mOptRecord)
            );
        }

        return processCachedOptimizerRecord;
    }

    public ProcessRecord(Object processRecord) {
        this.processRecord = processRecord;
        this.pid = getPid(processRecord);
        this.uid = getUID(processRecord);
        this.userId = getUserId(processRecord);

        ApplicationInfo applicationInfo = (ApplicationInfo) XposedHelpers.getObjectField(processRecord, FieldConstants.info);
        this.packageName = applicationInfo.packageName;

        this.processName = getProcessName(processRecord);
//        this.processStateRecord = XposedHelpers.getObjectField(processRecord, FieldConstants.mState);
        this.processStateRecord = new ProcessStateRecord(XposedHelpers.getObjectField(processRecord, FieldConstants.mState));
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
     * @param processRecord 安卓的ProcessRecord 对象
     * @return 进程名
     */
    public static String getProcessName(Object processRecord) {
        return (String) XposedHelpers.getObjectField(processRecord, FieldConstants.processName);
    }

    public static boolean isProcessNameSame(String expectProcName, Object processRecord) {
        return expectProcName.equals(getProcessName(processRecord));
    }

    public static int getUID(Object processRecord) {
        return XposedHelpers.getIntField(processRecord, FieldConstants.uid);
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
        boolean setSucceed = false;
        try {
            this.processStateRecord.setMaxAdj(maxAdj);
            setSucceed = true;
        } catch (Throwable t) {
            try {
                XposedHelpers.setIntField(
                        this.processStateRecord.getProcessStateRecord(),
                        FieldConstants.mMaxAdj,
                        maxAdj);
                setSucceed = true;
            } catch (Throwable ignore) {
            }
        }

        this.recordMaxAdj = setSucceed ? maxAdj : ProcessList.UNKNOWN_ADJ;
    }

    /**
     * 获取进程的最大adj
     *
     * @return 进程的最大adj
     */
    public int getMaxAdj() {
        try {
            return this.processStateRecord.getMaxAdj();
        } catch (Throwable t) {
            try {
                return XposedHelpers.getIntField(
                        this.processStateRecord.getProcessStateRecord(), FieldConstants.mMaxAdj);
            } catch (Throwable th) {
                return ProcessList.UNKNOWN_ADJ;
            }
        }
    }

    /**
     * 获取已记录的最大adj
     */
    public int getRecordMaxAdj() {
        return this.recordMaxAdj;
    }

    /**
     * 获取pid
     *
     * @param processRecord 安卓ProcessRecord
     */
    public static int getPid(Object processRecord) {
        return XposedHelpers.getIntField(processRecord, FieldConstants.mPid);
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
        } catch (Throwable ignore) {
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
        return getMaxAdj() != this.recordMaxAdj;
    }

    /**
     * 如果当前最大adj不等于已记录的最大adj, 则进行调整
     */
    public void adjustMaxAdjIfNeed() {
        if (isNeedAdjustMaxAdj()) {
            setMaxAdj(this.recordMaxAdj);
        }
    }

    public int getCurAdj() {
        return (int) XposedHelpers.callMethod(this.processRecord, MethodConstants.getCurAdj);
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

    public String getPackageName() {
        return packageName;
    }

    public Object getProcessRecord() {
        return processRecord;
    }

    public ProcessStateRecord getProcessStateRecord() {
        return processStateRecord;
    }
}
