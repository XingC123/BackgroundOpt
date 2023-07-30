package com.venus.backgroundopt.entity;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    private int oomAdjScore = Integer.MIN_VALUE;

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    private int fixedOomAdjScore = Integer.MIN_VALUE;

    /* *************************************************************************
     *                                                                         *
     * 防止重复压缩                                                              *
     *                                                                         *
     **************************************************************************/
    private final AtomicReference<AppGroupEnum> lastAppGroupEnumAtomicReference = new AtomicReference<>();

    public AppGroupEnum getLastAppGroupEnum() {
        return lastAppGroupEnumAtomicReference.get();
    }

    public void setLastAppGroupEnum(AppGroupEnum appGroupEnum) {
        lastAppGroupEnumAtomicReference.set(appGroupEnum);
    }

    private volatile long lastCompactTime;

    public void setLastCompactTime(long lastCompactTime) {
        this.lastCompactTime = lastCompactTime;
    }

    private final long compactInterval;

    /**
     * 是否允许压缩
     *
     * @param curTime 当前时间戳
     * @return 允许 -> true
     */
    public boolean isAllowedCompact(long curTime) {
        return curTime - lastCompactTime >= compactInterval;
    }

    public ProcessInfo(ProcessRecord processRecord) {
        this(processRecord.getUid(), processRecord.getPid(), Integer.MIN_VALUE);
    }

    public ProcessInfo(int uid, int pid, int oomAdjScore) {
        this(uid, pid, oomAdjScore, Integer.MIN_VALUE);
    }

    public ProcessInfo(int uid, int pid, int oomAdjScore, int fixedOomAdjScore) {
        this.uid = uid;
        this.pid = pid;
        this.oomAdjScore = oomAdjScore;
        this.fixedOomAdjScore = fixedOomAdjScore;

        compactInterval = TimeUnit.SECONDS.toMillis(30);
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

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }
}
