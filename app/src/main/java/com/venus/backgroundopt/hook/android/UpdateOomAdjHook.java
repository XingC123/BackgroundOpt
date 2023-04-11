package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.entity.AppInfo;
import com.venus.backgroundopt.server.ProcessList;
import com.venus.backgroundopt.server.ProcessRecord;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 更新oom_adj的hook
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/9
 */
public class UpdateOomAdjHook extends MethodHook {
    public UpdateOomAdjHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ProcessList;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.setOomAdj;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                int pid = (int) param.args[0];
                int uid = (int) param.args[1];
                RunningInfo runningInfo = getRunningInfo();
                AppInfo appInfo = runningInfo.getRunningAppInfo(uid);

                if (appInfo != null) {  // 非系统重要进程
                    int oomAdj = (int) param.args[2];
                    // 更新的是主进程的oom_adj
                    if (pid == appInfo.getmPid()) {
                        // 主进程当前adj不为ProcessRecord.DEFAULT_MAIN_ADJ
//                        if (appInfo.getMainProcCurAdj() != ProcessRecord.DEFAULT_MAIN_ADJ) {
//                            param.args[2] = ProcessRecord.DEFAULT_MAIN_ADJ;
//                            appInfo.setMainProcCurAdj(ProcessRecord.DEFAULT_MAIN_ADJ);
//
//                            getLogger().debug(
//                                    "设置主进程[" + pid + "-" + appInfo.getPackageName() + "]adj: "
//                                            + param.args[2]);
//                        } else {    // 主进程已经设置为目标值, 则阻止新值的设置
//                            param.setResult(null);
//                        }

                        if (oomAdj <= ProcessRecord.DEFAULT_MAIN_ADJ) {
                            appInfo.setMainProcCurAdj(oomAdj);
                            getLogger().debug(
                                    "设置主进程[" + pid + "-" + appInfo.getPackageName() + "]adj: "
                                            + param.args[2]);
                        } else {
                            if (appInfo.getMainProcCurAdj() != ProcessRecord.DEFAULT_MAIN_ADJ) {
                                param.args[2] = ProcessRecord.DEFAULT_MAIN_ADJ;
                                appInfo.setMainProcCurAdj(ProcessRecord.DEFAULT_MAIN_ADJ);
                            } else {
                                param.setResult(null);
                            }
                        }
                    } else {    // 子进程的处理
//                        if (appInfo.getMainProcCurAdj() != ProcessList.FOREGROUND_APP_ADJ) {
                        // 子进程信息尚未记录
                        if (!runningInfo.isSubProcessRunning(pid)) {
                            param.args[2] = ProcessRecord.SUB_PROC_ADJ;
                            runningInfo.setSubProcessAdj(pid, ProcessRecord.SUB_PROC_ADJ);

                            getLogger().debug(
                                    "设置子进程[" + pid + "-" + appInfo.getPackageName() + "]adj: "
                                            + param.args[2]);
                        } else {
                            int curAdj = runningInfo.getSubProcessAdj(pid);
                            /*
                                新的adj大于已记录的adj 且 当前adj不为"不可能取值"(即 已收录当前pid的信息), 则只更新记录
                             */
                            if (oomAdj > curAdj && curAdj != ProcessList.IMPOSSIBLE_ADJ) {
                                runningInfo.setSubProcessAdj(pid, oomAdj);
                            } else {
                                param.setResult(null);
                            }
                        }
//                        }


                        // 若子进程当前要设置的adj小于预定值, 则纠正
//                        if (adj < ProcessRecord.SUB_PROC_ADJ) {
//                            param.args[2] = ProcessRecord.SUB_PROC_ADJ;
//                            getLogger().debug(
//                                    "设置子进程[" + pid + "-" + appInfo.getPackageName() + "]adj: "
//                                            + param.args[2]);
//                        }
                    }
                } /*else {    // 是系统重要进程
                    // 若该进程信息尚未被记录
                    if (!runningInfo.isImportantSysAppPidRunning(pid)) {
                        param.args[2] = ProcessList.NATIVE_ADJ;
                        runningInfo.setImportantSysAppAdj(pid, ProcessList.NATIVE_ADJ);
                    } else {    // 若已经被记录, 则已经被调整值, 无需再次调整
                        param.setResult(null);
                    }
                }*/
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        // pid, uid, oom_adj_score
        return new Object[]{int.class, int.class, int.class};
    }
}
