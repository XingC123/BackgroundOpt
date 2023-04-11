package com.venus.backgroundopt.hook.hook_handler;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.android.AMSHook;
import com.venus.backgroundopt.hook.android.AppSwitchHook;
import com.venus.backgroundopt.hook.android.KillAppHook;
import com.venus.backgroundopt.hook.android.UpdateOomAdjHook;
import com.venus.backgroundopt.hook.base.PackageHook;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/17
 */
public class AndroidHookHandle extends PackageHook {
    public AndroidHookHandle(XC_LoadPackage.LoadPackageParam packageParam) {
        super(packageParam);
    }

    @Override
    public String getTargetPackageName() {
        return "android";
    }

    @Override
    public void hook(XC_LoadPackage.LoadPackageParam packageParam) {
        RunningInfo runningInfo = new RunningInfo();

        // 抓取AMS
        new AMSHook(packageParam.classLoader, runningInfo);

        // 前后台切换
        new AppSwitchHook(packageParam.classLoader, runningInfo);

        // 杀后台hook
        new KillAppHook(packageParam.classLoader, runningInfo);

        // oom_adj更新hook
        new UpdateOomAdjHook(packageParam.classLoader, runningInfo);
    }
}
