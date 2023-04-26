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
    String ProcessList = "com.android.server.am.ProcessList";
    String ProcessRecord = "com.android.server.am.ProcessRecord";
    String ProcessStateRecord = "com.android.server.am.ProcessStateRecord";
    String OomAdjuster = "com.android.server.am.OomAdjuster";

    /**
     * miui
     */
    String PowerStateMachine = "com.miui.powerkeeper.statemachine.PowerStateMachine";
    String SleepModeControllerNew = "com.miui.powerkeeper.statemachine.SleepModeControllerNew";
    String ProcessManager = "miui.process.ProcessManager";
    String ProcessConfig = "miui.process.ProcessConfig";
}
