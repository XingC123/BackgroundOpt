package com.venus.backgroundopt.hook.handle.android;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class ProcessListHook extends MethodHook {
    public ProcessListHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.ProcessList,
                        MethodConstants.setOomAdj,
                        new HookAction[]{
                                (BeforeHookAction) this::handleSetOomAdj
                        },
                        // pid, uid, oom_adj_score
                        int.class,
                        int.class,
                        int.class
                ),
        };
    }

    private Object handleSetOomAdj(XC_MethodHook.MethodHookParam param) {
        int uid = (int) param.args[1];
        RunningInfo runningInfo = getRunningInfo();
        AppInfo appInfo = runningInfo.getRunningAppInfo(uid);

        if (appInfo != null) {  // 非系统重要进程
            int pid = (int) param.args[0];
            int oomAdjScore = (int) param.args[2];

            if (pid == appInfo.getmPid()) { // 主进程
                if (appInfo.getMainProcCurAdj() != ProcessRecord.DEFAULT_MAIN_ADJ) {    // 第一次打开app
                    param.args[2] = ProcessRecord.DEFAULT_MAIN_ADJ;
                    appInfo.setMainProcCurAdj(ProcessRecord.DEFAULT_MAIN_ADJ);

                    if (BuildConfig.DEBUG) {
                        getLogger().debug(
                                "设置主进程[" + pid + "-" + appInfo.getPackageName() + "]adj: "
                                        + param.args[2]);
                    }
                } else {
                    param.setResult(null);
                }
            } else { // 子进程的处理
                // 子进程信息尚未记录
                if (!runningInfo.isSubProcessRunning(pid)) {
                    param.args[2] = ProcessRecord.SUB_PROC_ADJ;
                    runningInfo.setSubProcessAdj(pid, ProcessRecord.SUB_PROC_ADJ);

                    if (BuildConfig.DEBUG) {
                        getLogger().debug(
                                "设置子进程[" + pid + "-" + appInfo.getPackageName() + "]adj: "
                                        + param.args[2]);
                    }
                } else {
                    int curAdj = runningInfo.getSubProcessAdj(pid);
                            /*
                                新的adj大于已记录的adj 且 当前adj不为"不可能取值"(即 已收录当前pid的信息), 则只更新记录
                             */
                    if (oomAdjScore > curAdj && curAdj != ProcessList.IMPOSSIBLE_ADJ) {
                        runningInfo.setSubProcessAdj(pid, oomAdjScore);
                    } else {
                        param.setResult(null);
                    }
                }
            }

            appInfo.modifyProcessInfoAndAddIfNull(pid, oomAdjScore);
        }

        return null;
    }
}
