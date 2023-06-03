package com.venus.backgroundopt.entity;

/**
 * 进程信息
 * 不同于{@link com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord}, 本类仅包含最基本信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public class ProcessInfo {
    private int pid;
    private int oomAdjScore;

    public ProcessInfo() {
    }

    public ProcessInfo(int pid, int oomAdjScore) {
        this.pid = pid;
        this.oomAdjScore = oomAdjScore;
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
}
