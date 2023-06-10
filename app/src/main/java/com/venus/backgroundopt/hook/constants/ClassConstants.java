package com.venus.backgroundopt.hook.constants;

/**
 * author: XingC
 * date: 2023/2/8
 * version: 1.0
 */
public interface ClassConstants {
    /**
     * 安卓
     */
    String ActivityManagerService = "com.android.server.am.ActivityManagerService";
    String ActivityManagerConstants = "com.android.server.am.ActivityManagerConstants";
    String PhantomProcessList = "com.android.server.am.PhantomProcessList";
    String RecentTasks = "com.android.server.wm.RecentTasks";
    String Task = "com.android.server.wm.Task";
    String ComponentName = "android.content.ComponentName";
    String IBinder = "android.os.IBinder";
    String Process = "android.os.Process";
    String SystemProperties = "android.os.SystemProperties";
    String ProcessList = "com.android.server.am.ProcessList";
    String ProcessRecord = "com.android.server.am.ProcessRecord";
    String ProcessStateRecord = "com.android.server.am.ProcessStateRecord";
    String OomAdjuster = "com.android.server.am.OomAdjuster";
    String DeviceConfig = "android.provider.DeviceConfig";
    String PackageManagerService = "com.android.server.pm.PackageManagerService";
    String PackageManagerServiceInjector = "com.android.server.pm.PackageManagerServiceInjector";
    String PackageManagerServiceTestParams = "com.android.server.pm.PackageManagerServiceTestParams";
    String SystemServer = "com.android.server.SystemServer";
    String TimingsTraceAndSlog = "com.android.server.utils.TimingsTraceAndSlog";
    String SystemServerDexLoadReporter = "com.android.server.pm.dex.SystemServerDexLoadReporter";
    String IPackageManager = "android.content.pm.IPackageManager";
    String CachedAppOptimizer = "com.android.server.am.CachedAppOptimizer";
    String ComponentCallbacks2 = "android.content.ComponentCallbacks2";
    String UidRecord = "com.android.server.am.UidRecord";

    /**
     * miui
     */
    String PowerStateMachine = "com.miui.powerkeeper.statemachine.PowerStateMachine";
    String SleepModeControllerNew = "com.miui.powerkeeper.statemachine.SleepModeControllerNew";
    String ProcessManager = "miui.process.ProcessManager";
    String ProcessConfig = "miui.process.ProcessConfig";
}
