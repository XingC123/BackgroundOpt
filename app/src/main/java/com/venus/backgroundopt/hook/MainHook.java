package com.venus.backgroundopt.hook;

import com.venus.backgroundopt.hook.handler.AndroidHookHandler;
import com.venus.backgroundopt.hook.handler.PowerKeeperHookHandler;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @date 2023/2/8
 * @version 1.0
 */
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        new AndroidHookHandler(loadPackageParam);
        // miui
        new PowerKeeperHookHandler(loadPackageParam);
    }
}
