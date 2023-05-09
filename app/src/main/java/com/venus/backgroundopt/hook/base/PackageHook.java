package com.venus.backgroundopt.hook.base;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class PackageHook {
    public PackageHook(XC_LoadPackage.LoadPackageParam packageParam) {
//        if (packageParam.packageName.equals(getTargetPackageName())) {
        hook(packageParam);
//        }
    }

    public abstract String getTargetPackageName();

    public abstract void hook(XC_LoadPackage.LoadPackageParam packageParam);
}
