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
    String getInstalledPackages = "getInstalledPackages";
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
    String getBoolean = "getBoolean";
    String getLong = "getLong";
    String setOverrideMaxCachedProcesses = "setOverrideMaxCachedProcesses";
    String updateMaxCachedProcesses = "updateMaxCachedProcesses";
    String updateMaxPhantomProcesses = "updateMaxPhantomProcesses";
    String getProperty = "getProperty";
    String sendSignal = "sendSignal";
    String getActiveLauncherPackageName = "getActiveLauncherPackageName";
    String systemReady = "systemReady";
    String startBootstrapServices = "startBootstrapServices";
    String main = "main";
    String getRoleHoldersAsUser = "getRoleHoldersAsUser";
    String getDefaultAppProvider = "getDefaultAppProvider";
    String getDefaultHome = "getDefaultHome";
    String getDefaultBrowser = "getDefaultBrowser";
    String configureSystemServerDexReporter = "configureSystemServerDexReporter";
    String isFirstBoot = "isFirstBoot";
    String readProcLines = "readProcLines";
    String readProcFile = "readProcFile";
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
    String cleanUpApplicationRecordLocked = "cleanUpApplicationRecordLocked";
    String onAddRoleHolder = "onAddRoleHolder";
    String killProcessesBelowAdj = "killProcessesBelowAdj";
    String trimInactiveRecentTasks = "trimInactiveRecentTasks";
    String setCurRawAdj = "setCurRawAdj";
    String readMemoryStatFromFilesystem = "readMemoryStatFromFilesystem";
    String getInteger = "getInteger";
    String removeLruProcessLocked = "removeLruProcessLocked";
    String handleProcessStartedLocked = "handleProcessStartedLocked";
    String performIdleMaintenance = "performIdleMaintenance";
    String updateAndTrimProcessLSP = "updateAndTrimProcessLSP";
    String handleUpdateAndTrimProcessLSP = "handleUpdateAndTrimProcessLSP";
    String removePidLocked = "removePidLocked";
    String startService = "startService";
    String updateLowMemStateLSP = "updateLowMemStateLSP";
    String killAppIfBgRestrictedAndCachedIdleLocked = "killAppIfBgRestrictedAndCachedIdleLocked";
    String setMemFactorOverride = "setMemFactorOverride";
    String isLastMemoryLevelNormal = "isLastMemoryLevelNormal";
    String getLastMemoryLevelLocked = "getLastMemoryLevelLocked";
    String trimMemoryUiHiddenIfNecessaryLSP = "trimMemoryUiHiddenIfNecessaryLSP";
    String waitForPressure = "waitForPressure";
    String getMemFactor = "getMemFactor";
    String run = "run";
    String isAvailable = "isAvailable";
    String shouldKillExcessiveProcesses = "shouldKillExcessiveProcesses";
    String onOomAdjustChanged = "onOomAdjustChanged";
    String setSetAdj = "setSetAdj";
    String getSetAdj = "getSetAdj";
    String addService = "addService";
    String useCompaction = "useCompaction";
    String updateFreezerDebounceTimeout = "updateFreezerDebounceTimeout";
    String removeRecentTasksByPackageName = "removeRecentTasksByPackageName";
    String cleanupRecentTasksForUser = "cleanupRecentTasksForUser";
    String cleanUpRemovedTask = "cleanUpRemovedTask";
    String cleanUpRemovedTaskLocked = "cleanUpRemovedTaskLocked";
    String acquireWakeLockInternal = "acquireWakeLockInternal";
    String releaseWakeLockInternal = "releaseWakeLockInternal";
    String setCached = "setCached";
    String getCached = "getCached";
    String setEmpty = "setEmpty";
    String getEmpty = "getEmpty";
    String shouldKillProcessForRemovedTask = "shouldKillProcessForRemovedTask";
    String setHasForegroundServices = "setHasForegroundServices";
    String hasForegroundServices = "hasForegroundServices";
    String numberOfCurReceivers = "numberOfCurReceivers";
    String killProcessesForRemovedTask = "killProcessesForRemovedTask";
    String killLocked = "killLocked";
    String setWaitingToKill = "setWaitingToKill";
    String getSetRawAdj = "getSetRawAdj";
    String getCurRawAdj = "getCurRawAdj";
    String isKilledByAm = "isKilledByAm";
    String putStringForUser = "putStringForUser";
    String setCurProcState = "setCurProcState";
    String getCurProcState = "getCurProcState";
    String getCompletedAdjSeq = "getCompletedAdjSeq";
    String isPendingFinishAttach = "isPendingFinishAttach";
    String getIsolatedEntryPoint = "getIsolatedEntryPoint";
    String numberOfRunningServices = "numberOfRunningServices";
    String isSdkSandbox = "isSdkSandbox";
    String getActiveInstrumentation = "getActiveInstrumentation";
    String updateAppUidRecLSP = "updateAppUidRecLSP";
    String getLruProcessesLOSP = "getLruProcessesLOSP";

    /**
     * miui
     */
    String clearAppWhenScreenOffTimeOut = "clearAppWhenScreenOffTimeOut";
    String clearAppWhenScreenOffTimeOutInNight = "clearAppWhenScreenOffTimeOutInNight";
    String clearApp = "clearApp";
    String kill = "kill";
}