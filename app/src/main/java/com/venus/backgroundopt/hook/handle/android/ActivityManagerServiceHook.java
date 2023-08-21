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
        ActivityManagerService ams = new ActivityManagerService(param.thisObject, classLoader);

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

        // 本次事件包名
        String packageName = ((ComponentName) args[0]).getPackageName();
        if (packageName == null) {
            return null;
        }

        // 本次事件用户
        int userId = (int) args[1];

        RunningInfo runningInfo = getRunningInfo();
        // 检查是否是系统重要进程
        RunningInfo.NormalAppResult normalAppResult = runningInfo.isNormalApp(userId, packageName);
        if (!normalAppResult.isNormalApp()) {
            return null;
        }

        AppInfo appInfo = runningInfo.computeRunningAppIfAbsent(userId, packageName, normalAppResult.getApplicationInfo().uid);

        // 更新app的切换状态
        appInfo.setAppSwitchEvent(event);

        if (event == ACTIVITY_RESUMED) {
            runningInfo.putIntoActiveAppGroup(appInfo);
        } else {
            runningInfo.putIntoTmpAppGroup(appInfo);
        }

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
