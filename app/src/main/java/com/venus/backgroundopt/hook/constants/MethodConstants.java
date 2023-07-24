package com.venus.backgroundopt.hook.constants;

/**
 * author: XingC
 * date: 2023/2/8
 * version: 1.0
 */
public interface MethodConstants {
    /**
     * android
     */
    String updateActivityUsageStats = "updateActivityUsageStats";
    String computeOomAdjLSP = "computeOomAdjLSP";
    String setSystemProcess = "setSystemProcess";
    String killProcessGroup = "killProcessGroup";
    String forceStopPackage = "forceStopPackage";
    String setOomAdj = "setOomAdj";
    String setCurAdj = "setCurAdj";
    String applyOomAdjLocked = "applyOomAdjLocked";
    String updateOomAdjLocked = "updateOomAdjLocked";
    String updateOomAdjLSP = "updateOomAdjLSP";
    String getApplicationInfoAsUser = "getApplicationInfoAsUser";
    String isAppForeground = "isAppForeground";
    String getTopApp = "getTopApp";
    String setMaxAdj = "setMaxAdj";
    String getMaxAdj = "getMaxAdj";
    String getUidForPid = "getUidForPid";
    String getParentPid = "getParentPid";
    String checkExcessivePowerUsage = "checkExcessivePowerUsage";
    String checkExcessivePowerUsageLPr = "checkExcessivePowerUsageLPr";
    String trimPhantomProcessesIfNecessary = "trimPhantomProcessesIfNecessary";
    String isInVisibleRange = "isInVisibleRange";
    String set = "set";

    String get = "get";
    String setOverrideMaxCachedProcesses = "setOverrideMaxCachedProcesses";
    String getProperty = "getProperty";
    String sendSignal = "sendSignal";
    String getActiveLauncherPackageName = "getActiveLauncherPackageName";
    String systemReady = "systemReady";
    String startBootstrapServices = "startBootstrapServices";
    String main = "main";
    String getDefaultAppProvider = "getDefaultAppProvider";
    String getDefaultHome = "getDefaultHome";
    String configureSystemServerDexReporter = "configureSystemServerDexReporter";
    String isFirstBoot = "isFirstBoot";
    String readProcLines = "readProcLines";
    String getFreeMemory = "getFreeMemory";
    String getTotalMemory = "getTotalMemory";
    String setSwappiness = "setSwappiness";
    String applyOomAdjLSP = "applyOomAdjLSP";
    String compactApp = "compactApp";   // (ProcessRecord app, boolean force, String compactRequestType)
    String compactAppFull = "compactAppFull";   // (ProcessRecord, boolean force)
    String compactProcess = "compactProcess";
    String scheduleTrimMemory = "scheduleTrimMemory";
    String findProcessLOSP = "findProcessLOSP";
    String getThread = "getThread";
    String getProcessRecordLocked = "getProcessRecordLocked";
    String getProcessInPackage = "getProcessInPackage";
    String setProcessGroup = "setProcessGroup";
    String getProcessGroup = "getProcessGroup";
    String setCurrentSchedulingGroup = "setCurrentSchedulingGroup";
    String setSetSchedGroup = "setSetSchedGroup";
    String hasForegroundActivities = "hasForegroundActivities";
    String setThreadGroupAndCpuset = "setThreadGroupAndCpuset";
    String getCurAdj = "getCurAdj";
    String setReqCompactAction = "setReqCompactAction";
    String updateUseCompaction = "updateUseCompaction";
    String deletePackageX = "deletePackageX";
    String deletePackageLIF = "deletePackageLIF";

    /**
     * miui
     */
    String clearAppWhenScreenOffTimeOut = "clearAppWhenScreenOffTimeOut";
    String clearAppWhenScreenOffTimeOutInNight = "clearAppWhenScreenOffTimeOutInNight";
    String clearApp = "clearApp";
    String kill = "kill";
}
