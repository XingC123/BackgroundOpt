package com.venus.backgroundopt.hook.android;

import android.content.Context;
import android.os.Handler;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.ConstructorHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/25
 */
public class ActivityManagerConstantsHook extends ConstructorHook {
    public ActivityManagerConstantsHook(ClassLoader classLoader) {
        super(classLoader);
    }

    public ActivityManagerConstantsHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.ActivityManagerConstants;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                setMCustomizedMaxCachedProcesses(param);
                setCUR_MAX_CACHED_PROCESSES(param);
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{Context.class, ClassConstants.ActivityManagerService, Handler.class};
    }

    /**
     * 避免因已缓存应用数量过多而导致杀后台事件发生
     */
    private void setCUR_MAX_CACHED_PROCESSES(MethodHookParam param) {
        setValue(param, FieldConstants.CUR_MAX_CACHED_PROCESSES, Integer.MAX_VALUE);
    }

    private void setMCustomizedMaxCachedProcesses(MethodHookParam param) {
        setValue(param, FieldConstants.mCustomizedMaxCachedProcesses, Integer.MAX_VALUE);
    }

    private void setValue(MethodHookParam param, String field, Object value) {
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
}
