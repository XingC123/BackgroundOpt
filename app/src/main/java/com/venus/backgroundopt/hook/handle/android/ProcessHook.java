package com.venus.backgroundopt.hook.handle.android;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.entity.AppInfo;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class ProcessHook extends MethodHook {
    public ProcessHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.Process,
                        MethodConstants.killProcessGroup,
                        new HookAction[]{
                                (BeforeHookAction) this::handleKillApp
                        },
                        int.class,
                        int.class
                ),
        };
    }

    private Object handleKillApp(XC_MethodHook.MethodHookParam param) {
        int uid = (int) param.args[0];
        int pid = (int) param.args[1];

        RunningInfo runningInfo = getRunningInfo();
        AppInfo appInfo = runningInfo.getRunningAppInfo(uid);
        if (appInfo != null) {
            // 有问题。有时候会只杀主进程，但子进程仍然存在，且oom_adj=-1000
                    /*
                        分析: 主进程被杀导致runningInfo.removeRunningApp(appInfo);
                        使得UpdateOomAdjHook: if (appInfo == null) 从而设置为-1000
                        问题: 为什么主进程被杀而子进程无事。目标应该是内存不足时杀子进程以保留更多主进程
                        后续(23.3.6): 设置进程最大adj之后解决
                     */
            if (pid == appInfo.getmPid()) {
                runningInfo.removeRunningApp(appInfo);

                if (BuildConfig.DEBUG) {
                    getLogger().debug("kill: " + appInfo.getPackageName());
                }
            } else {
                runningInfo.removeSubProcessPid(pid, appInfo);
                appInfo.removeProcessRecord(pid);

                if (BuildConfig.DEBUG) {
                    getLogger().debug(appInfo.getPackageName() + " 的子进程被杀");
                }
            }
        } /*else {
            runningInfo.removeImportantSysAppPid(pid);
        }*/
//                String pkgName = (String) param.args[0];
//                getRunningInfo().removeRunningApp(pkgName);
//                getLogger().info("kill " + pkgName);

        return null;
    }
}
