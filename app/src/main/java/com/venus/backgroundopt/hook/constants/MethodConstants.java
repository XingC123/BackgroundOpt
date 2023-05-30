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
    String getUidForPid = "getUidForPid";
    String getParentPid = "getParentPid";
    String checkExcessivePowerUsage = "checkExcessivePowerUsage";
    String checkExcessivePowerUsageLPr = "checkExcessivePowerUsageLPr";
    String trimPhantomProcessesIfNecessary = "trimPhantomProcessesIfNecessary";
    String isInVisibleRange = "isInVisibleRange";
    String set = "set";
    String setOverrideMaxCachedProcesses = "setOverrideMaxCachedProcesses";
    String getProperty = "getProperty";
    String sendSignal = "sendSignal";
    String getActiveLauncherPackageName = "getActiveLauncherPackageName";
    String systemReady = "systemReady";
    String startBootstrapServices = "startBootstrapServices";
    String main = "main";
    String getDefaultAppProvider = "getDefaultAppProvider";
    String getDefaultHome = "getDefaultHome";

    /**
     * miui
     */
    String clearAppWhenScreenOffTimeOut = "clearAppWhenScreenOffTimeOut";
    String clearAppWhenScreenOffTimeOutInNight = "clearAppWhenScreenOffTimeOutInNight";
    String clearApp = "clearApp";
    String kill = "kill";
}
