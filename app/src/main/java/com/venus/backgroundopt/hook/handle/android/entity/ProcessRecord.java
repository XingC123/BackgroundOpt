package com.venus.backgroundopt.hook.handle.android.entity;

import android.content.pm.ApplicationInfo;

import com.alibaba.fastjson2.annotation.JSONField;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.entity.base.BaseProcessInfo;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#ProcessRecord}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class ProcessRecord extends BaseProcessInfo {
    // 默认的最大adj
    public static final int DEFAULT_MAX_ADJ = ProcessList.VISIBLE_APP_ADJ;
    // 默认的主进程要设置的adj
    public static final int DEFAULT_MAIN_ADJ = ProcessList.FOREGROUND_APP_ADJ;
    // 默认的子进程要设置的adj
    public static final int SUB_PROC_ADJ = DEFAULT_MAX_ADJ + 1;

    // 反射拿到的安卓的processRecord对象
    @JSONField(serialize = false)
    private Object processRecord;
    // 反射拿到的安卓的processStateRecord对象
    @JSONField(serialize = false)
    private ProcessStateRecord processStateRecord;
    // 当前ProcessRecord已记录的最大adj
    private int recordMaxAdj;

    /**
     * All about the state info of the optimizer when the process is cached.
     */
    @JSONField(serialize = false)
    private ProcessCachedOptimizerRecord processCachedOptimizerRecord;

    public ProcessCachedOptimizerRecord getProcessCachedOptimizerRecord() {
        if (processCachedOptimizerRecord == null) {
            processCachedOptimizerRecord = new ProcessCachedOptimizerRecord(
                    XposedHelpers.getObjectField(this.processRecord, FieldConstants.mOptRecord)
            );
        }

        return processCachedOptimizerRecord;
    }

    public ProcessRecord() {
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

    public static ProcessRecord newInstance(RunningInfo runningInfo, AppInfo appInfo, Object processRecord) {
        ProcessRecord record = new ProcessRecord(processRecord);
        setMainProcess(appInfo, record);

        addCompactProcess(runningInfo, appInfo, record);

        return record;
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
    @JSONField(serialize = false)
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
    @JSONField(serialize = false)
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
    @JSONField(serialize = false)
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
    @JSONField(serialize = false)
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

    @JSONField(serialize = false)
    public int getCurAdj() {
        return (int) XposedHelpers.callMethod(this.processRecord, MethodConstants.getCurAdj);
    }

    /* *************************************************************************
     *                                                                         *
     * 独立于安卓原本ProcessRecord的字段                                           *
     *                                                                         *
     **************************************************************************/
    // 此oomAdjScore为模块设置的值。若当前非app主进程, 则此值和真实值保持一致
    @JSONField(serialize = false)
    private final AtomicInteger oomAdjScore = new AtomicInteger(Integer.MIN_VALUE);
    // 上次压缩时间
    @JSONField(serialize = false)
    private final AtomicLong lastCompactTime = new AtomicLong(0L);
    // 压缩间隔
    private static final long compactInterval = TimeUnit.MINUTES.toMillis(7);

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    private int fixedOomAdjScore = Integer.MIN_VALUE;

    private boolean mainProcess = false;    // app主进程

    private void addCompactProcess(RunningInfo runningInfo) {
        runningInfo.getProcessManager().addCompactProcess(this);
    }

    /**
     * 若该进程创建时, app处于IDLE, 则将此进程添加到待压缩列表
     *
     * @param runningInfo   运行信息
     * @param appInfo       应用信息
     * @param processRecord 进程记录
     */
    public static void addCompactProcess(RunningInfo runningInfo, AppInfo appInfo, ProcessRecord processRecord) {
        if (Objects.equals(RunningInfo.AppGroupEnum.IDLE, appInfo.getAppGroupEnum())) { // 若该进程创建时, app处于IDLE, 则将此进程添加到待压缩列表
            processRecord.addCompactProcess(runningInfo);
        }
    }

    public boolean isAllowedCompact(long time) {
        return ((time - getLastCompactTime()) > compactInterval);
    }

    public int getOomAdjScore() {
        return oomAdjScore.get();
    }

    public void setOomAdjScore(int oomAdjScore) {
        this.oomAdjScore.set(oomAdjScore);
    }

    public long getLastCompactTime() {
        return lastCompactTime.get();
    }

    public void setLastCompactTime(long time) {
        lastCompactTime.set(time);
    }

    public int getFixedOomAdjScore() {
        return fixedOomAdjScore;
    }

    public void setFixedOomAdjScore(int fixedOomAdjScore) {
        this.fixedOomAdjScore = fixedOomAdjScore;
    }

    public boolean isMainProcess() {
        return mainProcess;
    }

    public void setMainProcess(boolean mainProcess) {
        this.mainProcess = mainProcess;
    }

    public static ProcessRecord setMainProcess(AppInfo appInfo, ProcessRecord processRecord) {
        try {
            processRecord.mainProcess = (processRecord.pid == appInfo.getmPid());
        } catch (Exception ignore) {
        }

        return processRecord;
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

    public Object getProcessRecord() {
        return processRecord;
    }

    public void setProcessRecord(Object processRecord) {
        this.processRecord = processRecord;
    }

    public ProcessStateRecord getProcessStateRecord() {
        return processStateRecord;
    }

    public void setProcessStateRecord(ProcessStateRecord processStateRecord) {
        this.processStateRecord = processStateRecord;
    }

    public int getRecordMaxAdj() {
        return this.recordMaxAdj;
    }

    public void setRecordMaxAdj(int recordMaxAdj) {
        this.recordMaxAdj = recordMaxAdj;
    }
}
