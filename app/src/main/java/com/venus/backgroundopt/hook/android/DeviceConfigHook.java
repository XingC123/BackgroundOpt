package com.venus.backgroundopt.hook.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/5/9
 */
public class DeviceConfigHook extends MethodHook {
    public static String NAMESPACE_ACTIVITY_MANAGER;

    public DeviceConfigHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);

        NAMESPACE_ACTIVITY_MANAGER =
                String.valueOf(XposedHelpers.getStaticObjectField(
                        XposedHelpers.findClass(ClassConstants.DeviceConfig, classLoader),
                        FieldConstants.NAMESPACE_ACTIVITY_MANAGER));
    }

    @Override
    public String getTargetClass() {
        return ClassConstants.DeviceConfig;
    }

    @Override
    public String getTargetMethod() {
        return MethodConstants.getProperty;
    }

    @Override
    public XC_MethodHook getActionMethod() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                Object namespace = param.args[0];
                Object name = param.args[1];
                if (Objects.equals(namespace, NAMESPACE_ACTIVITY_MANAGER) &&
                        Objects.equals(name, ActivityManagerConstantsHook.KEY_MAX_CACHED_PROCESSES)) {
                    param.setResult(String.valueOf(Integer.MAX_VALUE));
                }
            }
        };
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{String.class, String.class};
    }
}
