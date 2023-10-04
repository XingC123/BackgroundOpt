package com.venus.backgroundopt.hook.handler;

import com.venus.backgroundopt.hook.base.PackageHook;
import com.venus.backgroundopt.hook.handle.self.ModuleActiveStateHook;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @date 2023/9/29
 */
public class SelfHookHandler extends PackageHook {
    public SelfHookHandler(XC_LoadPackage.LoadPackageParam packageParam) {
        super(packageParam);
    }

    @Override
    public void hook(XC_LoadPackage.LoadPackageParam packageParam) {
        ClassLoader classLoader = packageParam.classLoader;
        new ModuleActiveStateHook(classLoader);
    }
}
