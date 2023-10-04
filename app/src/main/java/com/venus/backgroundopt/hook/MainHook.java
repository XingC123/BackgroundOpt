package com.venus.backgroundopt.hook;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handler.AndroidHookHandler;
import com.venus.backgroundopt.hook.handler.PowerKeeperHookHandler;
import com.venus.backgroundopt.hook.handler.SelfHookHandler;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        switch (loadPackageParam.packageName) {
            case "android" -> new AndroidHookHandler(loadPackageParam);
            // miui
            case "com.miui.powerkeeper" -> new PowerKeeperHookHandler(loadPackageParam);
            // 自己
            case BuildConfig.APPLICATION_ID -> new SelfHookHandler(loadPackageParam);
        }
    }
}
