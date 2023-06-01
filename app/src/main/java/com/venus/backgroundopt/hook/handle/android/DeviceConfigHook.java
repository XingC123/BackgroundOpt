package com.venus.backgroundopt.hook.handle.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
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
public class DeviceConfigHook extends MethodHook {
    public static String NAMESPACE_ACTIVITY_MANAGER;

    public DeviceConfigHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);

        NAMESPACE_ACTIVITY_MANAGER =
                String.valueOf(XposedHelpers.getStaticObjectField(
                        XposedHelpers.findClass(ClassConstants.DeviceConfig, classLoader),
                        FieldConstants.NAMESPACE_ACTIVITY_MANAGER));
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.DeviceConfig,
                        MethodConstants.getProperty,
                        new HookAction[]{
                                (BeforeHookAction) this::setKEY_MAX_CACHED_PROCESSES
                        },
                        String.class, String.class
                )
        };
    }

    private Object setKEY_MAX_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        Object namespace = param.args[0];
        Object name = param.args[1];
        if (Objects.equals(namespace, NAMESPACE_ACTIVITY_MANAGER) &&
                Objects.equals(name, ActivityManagerConstantsHook.KEY_MAX_CACHED_PROCESSES)) {
            param.setResult(String.valueOf(Integer.MAX_VALUE));
        }

        return null;
    }
}
