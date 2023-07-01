package com.venus.backgroundopt.hook.handle.android.entity;

/**
 * 封装了{@link android.content.pm.ApplicationInfo}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/14
 */
public class ApplicationInfo {
    private int repairedUid;

    public int getRepairedUid() {
        return repairedUid;
    }

    public ApplicationInfo setRepairedUid(int repairedUid) {
        this.repairedUid = repairedUid;

        return this;
    }

    private final android.content.pm.ApplicationInfo applicationInfo;

    public android.content.pm.ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public ApplicationInfo(android.content.pm.ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
        this.uid = applicationInfo.uid;
    }

    public int uid;
}