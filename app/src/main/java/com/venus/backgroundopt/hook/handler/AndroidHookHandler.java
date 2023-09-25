package com.venus.backgroundopt.hook.handler;

import android.os.Build;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.PackageHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerConstantsHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt;
import com.venus.backgroundopt.hook.handle.android.DeletePackageHelperHook;
import com.venus.backgroundopt.hook.handle.android.DeviceConfigHook;
import com.venus.backgroundopt.hook.handle.android.OomAdjusterHook;
import com.venus.backgroundopt.hook.handle.android.PackageManagerServiceHookKt;
import com.venus.backgroundopt.hook.handle.android.PhantomProcessListHook;
import com.venus.backgroundopt.hook.handle.android.ProcessListHookKt;
import com.venus.backgroundopt.hook.handle.android.RecentTasksHook;
import com.venus.backgroundopt.hook.handle.android.RoleControllerManagerHook;
import com.venus.backgroundopt.hook.handle.android.SystemPropertiesHook;

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
        RunningInfo runningInfo = new RunningInfo();
        ClassLoader classLoader = packageParam.classLoader;

        // hook获取
        new DeviceConfigHook(classLoader, runningInfo);

        // 抓取AMS, 前后台切换
        new ActivityManagerServiceHook(classLoader, runningInfo);
        new ActivityManagerServiceHookKt(classLoader, runningInfo);

        // 默认桌面
        new PackageManagerServiceHookKt(classLoader, runningInfo);

        // 杀后台hook
//        new ProcessHook(classLoader, runningInfo);

        // oom_adj更新hook
        new OomAdjusterHook(classLoader, runningInfo);
        new ProcessListHookKt(classLoader, runningInfo);

        // 安卓虚进程处理hook
        new PhantomProcessListHook(classLoader, runningInfo);

        new ActivityManagerConstantsHook(classLoader, runningInfo);

        // 最近任务可见性hook
        new RecentTasksHook(classLoader, runningInfo);

        // 软件卸载
        // 安卓12在PackageManagerServiceHook完成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // 安卓13
            new DeletePackageHelperHook(classLoader, runningInfo);
        }

        new SystemPropertiesHook(classLoader, runningInfo);

        new RoleControllerManagerHook(classLoader, runningInfo);
    }
}
