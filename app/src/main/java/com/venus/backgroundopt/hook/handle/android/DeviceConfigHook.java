package com.venus.backgroundopt.hook.handle.android;

import static com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerConstants.KEY_MAX_CACHED_PROCESSES;
import static com.venus.backgroundopt.hook.handle.android.entity.DeviceConfig.NAMESPACE_ACTIVITY_MANAGER;

import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class DeviceConfigHook extends MethodHook {
    public DeviceConfigHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
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
        if (Objects.equals(NAMESPACE_ACTIVITY_MANAGER, namespace)) {
            if (Objects.equals(KEY_MAX_CACHED_PROCESSES, name)) {
                param.setResult(String.valueOf(Integer.MAX_VALUE));
            }
        }

        return null;
    }
}
