package com.venus.backgroundopt.hook.handle.android;

import static com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.ProcessInfo;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.handle.android.entity.Process;
import com.venus.backgroundopt.manager.process.ProcessManager;

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
//                new HookPoint(
//                        ClassConstants.Process,
//                        MethodConstants.setProcessGroup,
//                        new HookAction[]{
//                                (BeforeHookAction) this::handleSetProcessGroup
//                        },
//                        /* pid, group */
//                        int.class,
//                        int.class
//                ),
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
            int mPid = Integer.MIN_VALUE;
            boolean flag = false;

            try {
                mPid = appInfo.getmPid();
            } catch (Exception e) {
                // app已清理过一次后台
                flag = true;
            }

            if (pid == mPid || flag) {
                runningInfo.removeRunningApp(appInfo);

                if (BuildConfig.DEBUG) {
                    getLogger().debug("kill: " + appInfo.getPackageName() + ", uid: " + uid);
                }
            } else if (mPid == Integer.MIN_VALUE) {
                if (BuildConfig.DEBUG) {
                    getLogger().warn("再次kill: " + appInfo.getPackageName() + ", uid: " + uid);
                }
            } else {
                // 移除进程记录
                ProcessInfo processInfo = appInfo.removeProcessInfo(pid);
                // 取消进程的待压缩任务
                runningInfo.getProcessManager().cancelCompactProcessInfo(processInfo);

                if (BuildConfig.DEBUG) {
                    getLogger().debug("[" + appInfo.getPackageName() + ", uid: " + uid + " ]的子进程被杀");
                }
            }
        }
//                String pkgName = (String) param.args[0];
//                getRunningInfo().removeRunningApp(pkgName);
//                getLogger().info("kill " + pkgName);

        return null;
    }

    private Object handleSetProcessGroup(XC_MethodHook.MethodHookParam param) {
        Object[] args = param.args;
//        RunningInfo runningInfo = getRunningInfo();

//        int pid = (int) args[0];
//        int uid = Process.getUidForPid(pid);
//        AppInfo appInfo = runningInfo.getRunningAppInfo(uid);
//
//        if (appInfo != null && appInfo.appGroupEnum == RunningInfo.AppGroupEnum.IDLE) {
//            args[1] = Process.THREAD_GROUP_BACKGROUND;
//
//            if (BuildConfig.DEBUG) {
//                getLogger().debug(appInfo.getPackageName() + "的pid=" + pid + "设置ProcessGroup: " + args[1]);
//            }
//        }

        int pid = (int) args[0];
        int group = (int) args[1];

        if (group > Process.THREAD_GROUP_RESTRICTED) {  //若是模块控制的行为, 则直接处理
            args[1] = group - ProcessManager.THREAD_GROUP_LEVEL_OFFSET;
            if (BuildConfig.DEBUG) {
                getLogger().debug(pid + "设置ProcessGroup >>> " + args[1]);
            }
        } else {    // 系统调用
            RunningInfo runningInfo = getRunningInfo();
            int uid = Process.getUidForPid(pid);
            AppInfo appInfo = runningInfo.getRunningAppInfo(uid);
            AppGroupEnum appGroupEnum;

            // 若此次行为发生时, app已进入后台(模块已经处理过)或处于tmp分组(模块会处理), 则不执行
            if (appInfo != null && (
                    (appGroupEnum = appInfo.getAppGroupEnum()) == AppGroupEnum.IDLE ||
                            appGroupEnum == AppGroupEnum.TMP)) {
                param.setResult(null);
            }
        }

        return null;
    }
}
