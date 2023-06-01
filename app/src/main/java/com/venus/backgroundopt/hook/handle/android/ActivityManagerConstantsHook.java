package com.venus.backgroundopt.hook.handle.android;

import android.content.Context;
import android.os.Handler;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.AfterHookAction;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class ActivityManagerConstantsHook extends MethodHook {
    /**
     * 退到一定时间, 允许使用的最大cpu占比
     */
    // <=5min
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_1 = 25;
    // (5, 10]
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_2 = 25;
    // (10, 15]
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_3 = 10;
    // >15
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_4 = 2;

    public static String KEY_MAX_CACHED_PROCESSES;

    public ActivityManagerConstantsHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);

        KEY_MAX_CACHED_PROCESSES =
                String.valueOf(XposedHelpers.getStaticObjectField(
                        XposedHelpers.findClass(ClassConstants.ActivityManagerConstants, classLoader),
                        FieldConstants.KEY_MAX_CACHED_PROCESSES));
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[] {
                new HookPoint(
                        ClassConstants.ActivityManagerConstants,
                        MethodConstants.setOverrideMaxCachedProcesses,
                        new HookAction[]{
                                (BeforeHookAction) this::handleSetOverrideMaxCachedProcesses
                        },
                        int.class
                ),
        };
    }

    @Override
    public HookPoint[] getConstructorHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.ActivityManagerConstants,
                        new HookAction[]{
                                (AfterHookAction) this::setAMCArgs
                        },
                        Context.class,
                        ClassConstants.ActivityManagerService,
                        Handler.class
                ),
        };
    }

    private Object setAMCArgs(XC_MethodHook.MethodHookParam param) {
        setMCustomizedMaxCachedProcesses(param);
        setCUR_MAX_CACHED_PROCESSES(param);
        setDEFAULT_MAX_CACHED_PROCESSES(param);
        setMOverrideMaxCachedProcesses(param);

        return null;
    }

    /**
     * 避免因已缓存应用数量过多而导致杀后台事件发生
     */
    private void setCUR_MAX_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.CUR_MAX_CACHED_PROCESSES, Integer.MAX_VALUE);
    }

    private void setMCustomizedMaxCachedProcesses(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.mCustomizedMaxCachedProcesses, Integer.MAX_VALUE);
    }

    private void setDEFAULT_MAX_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.DEFAULT_MAX_CACHED_PROCESSES, Integer.MAX_VALUE);
    }

    private void setMOverrideMaxCachedProcesses(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.mOverrideMaxCachedProcesses, Integer.MAX_VALUE);
    }

    private void setValue(XC_MethodHook.MethodHookParam param, String field, Object value) {
        try {
            XposedHelpers.setObjectField(param.thisObject, field, value);
            if (Objects.equals(XposedHelpers.getObjectField(param.thisObject, field), value)) {
                debugLog(isDebugMode() && getLogger().debug(field + "设置成功"));
            } else {
                getLogger().warn(field + "设置失败");
            }
        } catch (Exception e) {
            getLogger().warn(field + "设置出现异常");
        }
    }

    private Object handleSetOverrideMaxCachedProcesses(XC_MethodHook.MethodHookParam param) {
        param.args[0] = Integer.MAX_VALUE;

        return null;
    }
}
