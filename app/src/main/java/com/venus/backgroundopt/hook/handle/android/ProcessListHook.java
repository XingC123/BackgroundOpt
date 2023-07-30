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
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;

import java.util.Objects;

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

        if (appInfo == null) {
            return null;
        }

        // 非系统重要进程
        int pid = (int) param.args[0];
        int oomAdjScore = (int) param.args[2];
        ProcessInfo processInfo;

        int mPid = Integer.MIN_VALUE;
        try {
            mPid = appInfo.getmPid();
        } catch (Exception e) {
            runningInfo.addRunningApp(appInfo);

            try {
                mPid = appInfo.getmPid();

                if (BuildConfig.DEBUG) {
                    getLogger().debug("[" + appInfo.getPackageName() + ", uid: " + uid + "]的主进程信息补全完毕");
                }
            } catch (Exception ex) {
                if (BuildConfig.DEBUG) {
                    getLogger().warn("获取: [" + appInfo.getPackageName() + ", uid: " + uid + "] 的mPid出错", ex);
                }
            }
        }

        if (pid == mPid) { // 主进程
            if (appInfo.getMainProcCurAdj() != ProcessRecord.DEFAULT_MAIN_ADJ) {    // 第一次打开app
                param.args[2] = ProcessRecord.DEFAULT_MAIN_ADJ;

                processInfo = appInfo.getmProcessInfo();
                processInfo.setFixedOomAdjScore(ProcessRecord.DEFAULT_MAIN_ADJ);
                processInfo.setOomAdjScore(oomAdjScore);

                if (BuildConfig.DEBUG) {
                    getLogger().debug("设置主进程: [" + appInfo.getPackageName() + ", uid: " + uid
                            + "] ->>> pid: " + pid + ", adj: " + param.args[2]);
                }
            } else {
//                    ProcessRecord mProcessRecord = appInfo.getmProcessRecord();
//                    mProcessRecord.adjustMaxAdjIfNeed();

                param.setResult(null);
                appInfo.modifyProcessInfoAndAddIfNull(pid, oomAdjScore);
            }
        } else if (pid == Integer.MIN_VALUE) {
            if (BuildConfig.DEBUG) {
                getLogger().warn(appInfo.getPackageName() + ", uid: " + uid + " 的pid = " + pid + " 不符合规范, 无法添加至进程列表");
            }

            return null;
        } else { // 子进程的处理
            // 子进程信息尚未记录
            if (!appInfo.isRecordedProcessInfo(pid)) {
                param.args[2] = ProcessRecord.SUB_PROC_ADJ;

                processInfo = appInfo.addProcessInfo(pid, oomAdjScore);
                processInfo.setFixedOomAdjScore(ProcessRecord.SUB_PROC_ADJ);
                processInfo.setOomAdjScore(ProcessRecord.SUB_PROC_ADJ);

                if (Objects.equals(AppGroupEnum.IDLE, appInfo.getAppGroupEnum())) {
                    runningInfo.getProcessManager().setPidToBackgroundProcessGroup(pid, appInfo);
                }

                if (BuildConfig.DEBUG) {
                    getLogger().debug("设置子进程: [" + appInfo.getPackageName() + ", uid: " + uid
                            + "] ->>> pid: " + pid + ", adj: " + param.args[2]);
                }
            } else {
                ProcessInfo subProcessInfo = appInfo.getProcessInfo(pid);

                if (subProcessInfo == null) {
                    if (BuildConfig.DEBUG) {
                        getLogger().warn("子进程 [" + appInfo.getPackageName() + ", uid: " + uid + ", pid: " + pid + " ]为空, 无法调整oom");
                    }
                    return null;
                }

                int fixedOomAdjScore = subProcessInfo.getFixedOomAdjScore();
                // 新的oomAdj小于修正过的adj 或 修正过的adj为不可能取值
                if (oomAdjScore < fixedOomAdjScore || fixedOomAdjScore == ProcessList.IMPOSSIBLE_ADJ) {
                    param.setResult(null);
                }

                appInfo.modifyProcessInfoAndAddIfNull(pid, oomAdjScore);
            }
        }

        return null;
    }
}
