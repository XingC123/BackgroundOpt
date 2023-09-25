package com.venus.backgroundopt.entity.base;

/**
 * @author XingC
 * @date 2023/9/25
 */
public class BaseProcessInfo {
    // 程序的uid
    protected int uid = Integer.MIN_VALUE;
    // 该进程对应的pid
    protected int pid = Integer.MIN_VALUE;
    // 进程所属程序的用户id
    protected int userId;
    // 进程所在的程序的包名
    protected String packageName;
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
    protected String processName;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
