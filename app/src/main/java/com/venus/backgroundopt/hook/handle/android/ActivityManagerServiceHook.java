package com.venus.backgroundopt.hook.handle.android;

import android.app.usage.UsageEvents;
import android.content.ComponentName;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.base.action.ReplacementHookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerService;
import com.venus.backgroundopt.hook.handle.android.entity.CachedAppOptimizer;
import com.venus.backgroundopt.hook.handle.android.entity.Process;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord;
import com.venus.backgroundopt.manager.process.ProcessManager;

import java.util.Collection;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class ActivityManagerServiceHook extends MethodHook {
    /**
     * 进入前台.
     */
    public static final int ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED;
    /**
     * 进入后台.
     */
    public static final int ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED;

    /**
     * {@link android.os.Process#SIGNAL_USR1}
     */
    public static final int SIGNAL_10 = 10;

    public ActivityManagerServiceHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.setSystemProcess,
                        new HookAction[]{
                                (BeforeHookAction) this::getAMSObj
                        }
                ),
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.updateActivityUsageStats,
                        new HookAction[]{
                                (BeforeHookAction) this::handleAppSwitch
                        },
                        ClassConstants.ComponentName,   /* activity */
                        int.class,  /* userId */
                        int.class,  /* event */
                        ClassConstants.IBinder, /* appToken ActivityRecord's appToken */
                        ClassConstants.ComponentName    /* taskRoot Task's root */
                ),
//                new HookPoint(  /* 未执行 */
//                        ClassConstants.ActivityManagerService,
//                        MethodConstants.updateActivityUsageStats,
//                        new HookAction[]{
//                                (BeforeHookAction) this::handlePackageSwitch
//                        },
//                        String.class,   /* packageName */
//                        int.class,  /* userId */
//                        int.class   /* event */
//                ),
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.checkExcessivePowerUsageLPr,
                        new HookAction[]{
                                (ReplacementHookAction) this::handleCheckExcessivePowerUsageLPr
                        },
                        long.class,
                        boolean.class,
                        long.class,
                        String.class,
                        String.class,
                        int.class,
                        ClassConstants.ProcessRecord
                ),
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.checkExcessivePowerUsage,
                        new HookAction[]{
                                (ReplacementHookAction) this::handleCheckExcessivePowerUsage
                        }
                ),
        };
    }

    /**
     * 获取AMS对象
     */
    private Object getAMSObj(XC_MethodHook.MethodHookParam param) {
        RunningInfo runningInfo = getRunningInfo();
        ActivityManagerService ams = new ActivityManagerService(param.thisObject);

        runningInfo.setActivityManagerService(ams);
        runningInfo.initProcessManager();

        if (BuildConfig.DEBUG) {
            getLogger().debug("拿到AMS");
        }

        // 设置persist.sys.spc.enabled禁用小米的杀后台
        XposedHelpers.callStaticMethod(
                XposedHelpers.findClass(ClassConstants.SystemProperties, classLoader),
                MethodConstants.set,
                "persist.sys.spc.enabled", "false");

        return null;
    }

    /* *************************************************************************
     *                                                                         *
     * app切换事件                                                               *
     *                                                                         *
     **************************************************************************/

    /**
     * 处理app切换事件
     */
    private Object handleAppSwitch(XC_MethodHook.MethodHookParam param) {
        // 获取方法参数
        Object[] args = param.args;

        // 获取切换事件
        int event = (int) args[2];
        if (event != ACTIVITY_PAUSED && event != ACTIVITY_RESUMED) {
            return null;
        }

        // 本次事件用户
        int userId = (int) args[1];
        // 本次事件包名
        String packageName = ((ComponentName) args[0]).getPackageName();
        if (packageName == null) {
            return null;
        }

        RunningInfo runningInfo = getRunningInfo();
        // 检查是否是系统重要进程
        RunningInfo.NormalAppResult normalAppResult = runningInfo.isNormalApp(packageName);
        if (!normalAppResult.isNormalApp()) {
            return null;
        }

        // 第一次打开此app
        boolean firstRunning = false;
        int uid = normalAppResult.getApplicationInfo().uid;
        int fixedUid = AppInfo.getFixedUid(userId, uid);
        AppInfo appInfo = runningInfo.getAppInfoFromRunningApps(fixedUid);

        if (appInfo == null) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("创建新进程: " + packageName);
            }

            firstRunning = true;
            appInfo = new AppInfo(userId, packageName, runningInfo).setFixedUid(fixedUid);

            // 添加到运行app列表
            runningInfo.addRunningApp(appInfo);
        }

        // 更新app的切换状态
        appInfo.setAppSwitchEvent(event);

        if (event == ACTIVITY_RESUMED) {
            runningInfo.putIntoActiveAppGroup(appInfo, firstRunning);
        } else {
            runningInfo.putIntoTmpAppGroup(appInfo);
        }

        return null;
    }

    private void handleCurApp(AppInfo appInfo) {
        // 移除TrimMemory计时任务
        getRunningInfo().getProcessManager().removeBackgroundAppTrimTask(appInfo.getmProcessRecord());
    }

    private void handleLastApp(AppInfo appInfo) {
        if (appInfo == null) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("待执行app已被杀死, 不执行处理");
            }

            return;
        }

        if (Objects.equals(getRunningInfo().getActiveLaunchPackageName(), appInfo.getPackageName())) {
            if (BuildConfig.DEBUG) {
                getLogger().debug("当前操作的app为默认桌面, 不进行处理");
            }
            return;
        }

        if (appInfo.isSwitchEventHandled()) {
            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + " 的切换事件已经处理过");
            }
            return;
        }

        scheduleTrimMemory(appInfo);
        handleGC(appInfo);
//        compactApp(appInfo);

        appInfo.setSwitchEventHandled(true);
    }

    /**
     * 处理gc事件
     *
     * @param appInfo app信息
     */
    private void handleGC(AppInfo appInfo) {
        // kill -10 pid
        try {
            Process.sendSignal(appInfo.getmPid(), SIGNAL_10);

            if (BuildConfig.DEBUG) {
                getLogger().debug(appInfo.getPackageName() + " 触发gc, pid = " + appInfo.getmPid());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                getLogger().error(appInfo.getPackageName() + " 存在问题, 无法执行gc");
            }
        }
    }

    /**
     * app进程压缩
     *
     * @param appInfo app信息
     */
    private void compactApp(AppInfo appInfo) {
        /*
            遍历app的进程信息。
            对真实oomAdj(本模块保后台依靠调整oom, 但同时也记录了系统原生的oom值)大于指定数值的进程进行压缩
         */
        ProcessManager processManager = getRunningInfo().getProcessManager();
        Collection<ProcessRecord> processInfoList = appInfo.getProcessRecordList();

        if (processInfoList.size() == 0) {
            debugLog(isDebugMode() &&
                    getLogger().warn(appInfo.getPackageName() + ": 未找到进程, 不执行压缩"));
            return;
        }

        processInfoList.stream()
                .filter(processRecord -> processRecord.getOomAdjScore() > ProcessList.HOME_APP_ADJ)
                .forEach(processInfo -> processManager.compactApp(
                        processInfo.getPid(),
                        CachedAppOptimizer.COMPACT_ACTION_FULL)
                );

        if (BuildConfig.DEBUG) {
            getLogger().debug(appInfo.getPackageName() + ": 压缩流程执行完毕");
        }
    }

    /**
     * 设置内存回收等级
     * 参考: <a href="https://blog.csdn.net/omnispace/article/details/73320955">Android系统中的进程管理：内存的回收</a>
     *
     * @param appInfo app信息
     */
    private void scheduleTrimMemory(AppInfo appInfo) {
        ProcessRecord processRecord = appInfo.getmProcessRecord();
        // 当上个app被关闭后, 会执行Appinfo.clearAppInfo()，清空属性值
        if (processRecord == null) {
            return;
        }

        getRunningInfo().getProcessManager().startBackgroundAppTrimTask(appInfo.getmProcessRecord());
    }

    /* *************************************************************************
     *                                                                         *
     * app切换事件2                                                              *
     *                                                                         *
     **************************************************************************/
    private Object handlePackageSwitch(XC_MethodHook.MethodHookParam param) {
        Object[] args = param.args;

        String packageName = (String) args[0];
        int userId = (int) args[1];
        int event = (int) args[2];

        getLogger().debug("packageName: " + packageName + ", userId: " + userId + ", event: " + event);

        return null;
    }

    /* *************************************************************************
     *                                                                         *
     * 杀后台设置                                                                *
     *                                                                         *
     **************************************************************************/
    private Object handleCheckExcessivePowerUsageLPr(XC_MethodHook.MethodHookParam param) {
        return false;
    }

    private Object handleCheckExcessivePowerUsage(XC_MethodHook.MethodHookParam param) {
        return null;
    }
}
