package com.venus.backgroundopt.hook.handler;

import com.venus.backgroundopt.hook.base.PackageHook;
import com.venus.backgroundopt.hook.handle.miui.PowerStateMachineHook;
import com.venus.backgroundopt.hook.handle.miui.ProcessManagerHook;
import com.venus.backgroundopt.hook.handle.miui.SleepModeControllerNewHook;

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

        new PowerStateMachineHook(classLoader);
        new ProcessManagerHook(classLoader);
        new SleepModeControllerNewHook(classLoader);
    }

    @Override
    public String getTargetPackageName() {
        return "com.miui.powerkeeper";
    }
}
