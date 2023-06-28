package com.venus.backgroundopt.hook.handler;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.PackageHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerConstantsHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.DeviceConfigHook;
import com.venus.backgroundopt.hook.handle.android.PackageManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.PhantomProcessListHook;
import com.venus.backgroundopt.hook.handle.android.ProcessHook;
import com.venus.backgroundopt.hook.handle.android.ProcessListHook;
import com.venus.backgroundopt.hook.handle.android.RecentTasksHook;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/17
 */
//@HookPackageName("android")
public class AndroidHookHandler extends PackageHook {
    public AndroidHookHandler(XC_LoadPackage.LoadPackageParam packageParam) {
        super(packageParam);
    }

    @Override
    public void hook(XC_LoadPackage.LoadPackageParam packageParam) {
        RunningInfo runningInfo = new RunningInfo(packageParam.classLoader);

        // 抓取AMS, 前后台切换
        new ActivityManagerServiceHook(packageParam.classLoader, runningInfo);

        // 默认桌面
        new PackageManagerServiceHook(packageParam.classLoader, runningInfo);

        // 杀后台hook
        new ProcessHook(packageParam.classLoader, runningInfo);

        // oom_adj更新hook
        new ProcessListHook(packageParam.classLoader, runningInfo);

        /*
            根据酷安(don`t kill)作者提供思路所写: https://www.coolapk.com/feed/43786372?shareKey=ZGMyOWJjNjZlZGRmNjQ0NzdkOTI~&shareFrom=com.coolapk.market_13.1.2
         */
        // 安卓虚进程处理hook
        new PhantomProcessListHook(packageParam.classLoader, runningInfo);
        new ActivityManagerConstantsHook(packageParam.classLoader, runningInfo);

        // 最近任务可见性hook
        new RecentTasksHook(packageParam.classLoader, runningInfo);

        // hook获取
        new DeviceConfigHook(packageParam.classLoader, runningInfo);
    }
}
