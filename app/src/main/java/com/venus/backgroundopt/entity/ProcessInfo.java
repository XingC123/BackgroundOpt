package com.venus.backgroundopt.entity;

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 进程信息
 * 不同于{@link com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord}, 本类仅包含最基本信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public class ProcessInfo {
    private int repairedUid = Integer.MIN_VALUE;
    private int pid = Integer.MIN_VALUE;
    private int oomAdjScore = Integer.MIN_VALUE;

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    private int fixedOomAdjScore = Integer.MIN_VALUE;

    private long lastCompactTime = Long.MIN_VALUE;

    private long compactInterval = Long.MIN_VALUE;

    public ProcessInfo() {
    }

    public ProcessInfo(ProcessRecord processRecord) {
        this(processRecord.getUid(), processRecord.getPid(), Integer.MIN_VALUE);
    }

    public ProcessInfo(int repairedUid, int pid, int oomAdjScore) {
        this(repairedUid, pid, oomAdjScore, Integer.MIN_VALUE);
    }

    public ProcessInfo(int repairedUid, int pid, int oomAdjScore, int fixedOomAdjScore) {
        this.repairedUid = repairedUid;
        this.pid = pid;
        this.oomAdjScore = oomAdjScore;
        this.fixedOomAdjScore = fixedOomAdjScore;

        this.compactInterval = TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInfo that = (ProcessInfo) o;
        return repairedUid == that.repairedUid && pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(repairedUid, pid);
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getOomAdjScore() {
        return oomAdjScore;
    }

    public void setOomAdjScore(int oomAdjScore) {
        this.oomAdjScore = oomAdjScore;
    }

    public int getFixedOomAdjScore() {
        return fixedOomAdjScore;
    }

    public void setFixedOomAdjScore(int fixedOomAdjScore) {
        this.fixedOomAdjScore = fixedOomAdjScore;
    }

    public int getRepairedUid() {
        return repairedUid;
    }

    public void setRepairedUid(int repairedUid) {
        this.repairedUid = repairedUid;
    }

    public long getLastCompactTime() {
        return lastCompactTime;
    }

    public void setLastCompactTime(long lastCompactTime) {
        this.lastCompactTime = lastCompactTime;
    }

    public long getCompactInterval() {
        return compactInterval;
    }
}
