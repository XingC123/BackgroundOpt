package com.venus.backgroundopt.hook.handler;

import com.venus.backgroundopt.hook.base.PackageHook;
import com.venus.backgroundopt.hook.miui.ClearAppHook;
import com.venus.backgroundopt.hook.miui.ClearAppWhenScreenOffTimeOutHook;
import com.venus.backgroundopt.hook.miui.ClearAppWhenScreenOffTimeOutInNightHook;
import com.venus.backgroundopt.hook.miui.ProcessManagerHook;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/17
 */
public class PowerKeeperHookHandler extends PackageHook {
    public PowerKeeperHookHandler(XC_LoadPackage.LoadPackageParam packageParam) {
        super(packageParam);

    }

    @Override
    public void hook(XC_LoadPackage.LoadPackageParam packageParam) {
        ClassLoader classLoader = packageParam.classLoader;

        new ClearAppWhenScreenOffTimeOutHook(classLoader);
        new ClearAppWhenScreenOffTimeOutInNightHook(classLoader);
        new ClearAppHook(classLoader);
        new ProcessManagerHook(classLoader);
    }

    @Override
    public String getTargetPackageName() {
        return "com.miui.powerkeeper";
    }
}
