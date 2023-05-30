package com.venus.backgroundopt.hook.handler;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.android.AMSHook;
import com.venus.backgroundopt.hook.android.ActiveLauncherHook;
import com.venus.backgroundopt.hook.android.ActivityManagerConstantsHook;
import com.venus.backgroundopt.hook.android.AppSwitchHook;
import com.venus.backgroundopt.hook.android.DeviceConfigHook;
import com.venus.backgroundopt.hook.android.KillAppHook;
import com.venus.backgroundopt.hook.android.PhantomProcessHook;
import com.venus.backgroundopt.hook.android.PhantomProcessHook2;
import com.venus.backgroundopt.hook.android.PhantomProcessHook3;
import com.venus.backgroundopt.hook.android.PhantomProcessListHook;
import com.venus.backgroundopt.hook.android.RecentTasksHook;
import com.venus.backgroundopt.hook.android.UpdateOomAdjHook;
import com.venus.backgroundopt.hook.base.PackageHook;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/17
 */
public class AndroidHookHandler extends PackageHook {
    public AndroidHookHandler(XC_LoadPackage.LoadPackageParam packageParam) {
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

        // 默认桌面
        new ActiveLauncherHook(packageParam.classLoader, runningInfo);

        // 前后台切换
        new AppSwitchHook(packageParam.classLoader, runningInfo);

        // 杀后台hook
        new KillAppHook(packageParam.classLoader, runningInfo);

        // oom_adj更新hook
        new UpdateOomAdjHook(packageParam.classLoader, runningInfo);

        /*
            根据酷安(don`t kill)作者提供思路所写: https://www.coolapk.com/feed/43786372?shareKey=ZGMyOWJjNjZlZGRmNjQ0NzdkOTI~&shareFrom=com.coolapk.market_13.1.2
         */
        // 安卓虚进程处理hook
        new PhantomProcessHook(packageParam.classLoader, runningInfo);
        new PhantomProcessListHook(packageParam.classLoader, runningInfo);
        new ActivityManagerConstantsHook(packageParam.classLoader, runningInfo);

        // 最近任务可见性hook
        new RecentTasksHook(packageParam.classLoader, runningInfo);

        // hook获取
        new DeviceConfigHook(packageParam.classLoader, runningInfo);

        // 其他处理
        new PhantomProcessHook2(packageParam.classLoader, runningInfo);
        new PhantomProcessHook3(packageParam.classLoader, runningInfo);
    }
}
