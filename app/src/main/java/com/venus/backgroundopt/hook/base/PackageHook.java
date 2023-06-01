package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.environment.SystemProperties;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class PackageHook {
    private RunningInfo runningInfo;

    public PackageHook(XC_LoadPackage.LoadPackageParam packageParam) {
        if (packageParam.packageName.equals(getTargetPackageName())) {
            this.runningInfo = new RunningInfo();

            // 更新RunningInfo内的hook次数
            this.runningInfo.updateHookTimes();

            // 环境
            SystemProperties.loadSystemPropertiesClazz(packageParam.classLoader);

            // hook
            hook(packageParam);
        }
    }

    public abstract String getTargetPackageName();

    public abstract void hook(XC_LoadPackage.LoadPackageParam packageParam);

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }
}
