/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    String LocalService = "com.android.server.am.ActivityManagerService.LocalService";
    String ActivityManagerConstants = "com.android.server.am.ActivityManagerConstants";

    String MemoryStatUtil = "com.android.server.am.MemoryStatUtil";
    String MemoryStat = "com.android.server.am.MemoryStatUtil.MemoryStat";
    String PhantomProcessList = "com.android.server.am.PhantomProcessList";
    String ProcessCachedOptimizerRecord = "com.android.server.am.ProcessCachedOptimizerRecord";
    String ActivityManagerShellCommand = "com.android.server.am.ActivityManagerShellCommand";
    String LowMemDetector = "com.android.server.am.LowMemDetector";
    String LowMemThread = "com.android.server.am.LowMemDetector.LowMemThread";
    String AppProfiler = "com.android.server.am.AppProfiler";
    String ProcessServiceRecord = "com.android.server.am.ProcessServiceRecord";
    String RecentTasks = "com.android.server.wm.RecentTasks";
    String WindowProcessController = "com.android.server.wm.WindowProcessController";
    String Task = "com.android.server.wm.Task";
    String ActivityTaskSupervisor = "com.android.server.wm.ActivityTaskSupervisor";
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
    String MemCompactionHandler = "com.android.server.am.CachedAppOptimizer.MemCompactionHandler";
    String ComponentCallbacks2 = "android.content.ComponentCallbacks2";
    String UidRecord = "com.android.server.am.UidRecord";
    String DeletePackageHelper = "com.android.server.pm.DeletePackageHelper";
    String PackageRemovedInfo = "com.android.server.pm.PackageRemovedInfo";
    String PackageRemovedInfo_A12 = "com.android.server.pm.PackageManagerService.PackageRemovedInfo";
    String ParsedPackage = "com.android.server.pm.parsing.pkg.ParsedPackage";
    String RoleControllerManager = "android.app.role.RoleControllerManager";
    String RemoteCallback = "android.os.RemoteCallback";
    String IApplicationThread = "android.app.IApplicationThread";
    String ActivityId = "android.app.assist.ActivityId";
    String Resources = "android.content.res.Resources";
    String ServiceManager = "android.os.ServiceManager";
    String PowerManagerService = "com.android.server.power.PowerManagerService";
    String IWakeLockCallback = "android.os.IWakeLockCallback";
    String UserHandle = "android.os.UserHandle";
    String Settings_Secure = "android.provider.Settings.Secure";

    /**
     * miui
     */
    String PowerStateMachine = "com.miui.powerkeeper.statemachine.PowerStateMachine";
    String SleepModeControllerNew = "com.miui.powerkeeper.statemachine.SleepModeControllerNew";
    String ProcessManager = "miui.process.ProcessManager";
    String ProcessConfig = "miui.process.ProcessConfig";
}