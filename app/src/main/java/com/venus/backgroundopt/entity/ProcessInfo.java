package com.venus.backgroundopt.entity;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程信息
 * 不同于{@link com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord}, 本类仅包含最基本信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public class ProcessInfo {
    private int uid = Integer.MIN_VALUE;
    private int pid = Integer.MIN_VALUE;
    private final AtomicInteger oomAdjScore = new AtomicInteger(Integer.MIN_VALUE);
    // 上次压缩时间
    private final AtomicLong lastCompactTime = new AtomicLong(0L);
    // 压缩间隔
    private static final long compactInterval = TimeUnit.MINUTES.toMillis(7);

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    private int fixedOomAdjScore = Integer.MIN_VALUE;

    private boolean mainProcess = false;    // app主进程

    private ProcessInfo(ProcessRecord processRecord) {
        this(processRecord.getUid(), processRecord.getPid(), Integer.MIN_VALUE);
    }

    private ProcessInfo(int uid, int pid, int oomAdjScore) {
        this(uid, pid, oomAdjScore, Integer.MIN_VALUE);
    }

    private ProcessInfo(int uid, int pid, int oomAdjScore, int fixedOomAdjScore) {
        this.uid = uid;
        this.pid = pid;
        this.oomAdjScore.set(oomAdjScore);
        this.fixedOomAdjScore = fixedOomAdjScore;
    }

    public static ProcessInfo newInstance(AppInfo appInfo, RunningInfo runningInfo, ProcessRecord processRecord) {
        return newInstance(appInfo, runningInfo, processRecord.getUid(), processRecord.getPid(), Integer.MIN_VALUE);
    }

    public static ProcessInfo newInstance(AppInfo appInfo, RunningInfo runningInfo, int uid, int pid, int oomAdjScore) {
        ProcessInfo processInfo = new ProcessInfo(uid, pid, oomAdjScore, Integer.MIN_VALUE);
        try {
            processInfo.mainProcess = (pid == appInfo.getmPid());
        } catch (Exception ignore) {
        }

        if (Objects.equals(AppGroupEnum.IDLE, appInfo.getAppGroupEnum())) { // 若该进程创建时, app处于IDLE, 则将此进程添加到待压缩列表
            return processInfo.addCompactProcessInfo(runningInfo);
        }

        return processInfo;
    }

    private ProcessInfo addCompactProcessInfo(RunningInfo runningInfo) {
        runningInfo.getProcessManager().addCompactProcessInfo(this);
        return this;
    }

    public boolean isAllowedCompact(long time) {
        return ((time - getLastCompactTime()) > compactInterval);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInfo that = (ProcessInfo) o;
        return uid == that.uid && pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, pid);
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getOomAdjScore() {
        return oomAdjScore.get();
    }

    public void setOomAdjScore(int oomAdjScore) {
        this.oomAdjScore.set(oomAdjScore);
    }

    public int getFixedOomAdjScore() {
        return fixedOomAdjScore;
    }

    public void setFixedOomAdjScore(int fixedOomAdjScore) {
        this.fixedOomAdjScore = fixedOomAdjScore;
    }

    public int getUid() {
        return uid;
    }

    public ProcessInfo setUid(int uid) {
        this.uid = uid;
        return this;
    }

    public boolean isMainProcess() {
        return mainProcess;
    }

    public long getLastCompactTime() {
        return lastCompactTime.get();
    }

    public void setLastCompactTime(long time) {
        lastCompactTime.set(time);
    }
}
