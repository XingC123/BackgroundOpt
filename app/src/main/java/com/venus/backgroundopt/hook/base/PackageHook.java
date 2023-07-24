package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.annotation.HookPackageName;
import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.environment.SystemProperties;
import com.venus.backgroundopt.utils.log.ILogger;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class PackageHook implements ILogger {
    private final RunningInfo runningInfo;

    public PackageHook(XC_LoadPackage.LoadPackageParam packageParam) {
        this.runningInfo = new RunningInfo();

        // 更新RunningInfo内的hook次数
        this.runningInfo.updateHookTimes();

        // 环境
        SystemProperties.loadSystemPropertiesClazz(packageParam.classLoader);

        // hook
        hook(packageParam);
    }

    public static String getTargetPackageName(Class<?> aClass) {
        HookPackageName annotation = aClass.getAnnotation(HookPackageName.class);
        String hookPackageName;

        if (annotation == null || "".equals(hookPackageName = annotation.value())) {
            return aClass.getCanonicalName();
        }

        return hookPackageName;
    }

    public abstract void hook(XC_LoadPackage.LoadPackageParam packageParam);

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }
}
