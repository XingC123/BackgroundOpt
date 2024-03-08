package com.venus.backgroundopt.hook.constants;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/9
 */
public interface FieldConstants {
    String info = "info";
    String mContext = "mContext";
    String mProcessList = "mProcessList";
    String mLruProcesses = "mLruProcesses";
    String mPid = "mPid";
    String pid = "pid";
    String uid = "uid";
    String userId = "userId";
    String processName = "processName";
    String mState = "mState";
    String mMaxAdj = "mMaxAdj";
    String mCompactThrottleMinOomAdj = "mCompactThrottleMinOomAdj";
    String mCompactThrottleMaxOomAdj = "mCompactThrottleMaxOomAdj";
    String mFullCompactRequest = "mFullCompactRequest";
    String mOptRecord = "mOptRecord";
    String mCompactionHandler = "mCompactionHandler";
    String mUseCompaction = "mUseCompaction";
    String applicationInfo = "applicationInfo";
    String packageName = "packageName";
    String mApp = "mApp";
    /**
     * {@link ClassConstants#ActivityManagerConstants}
     */
    String CUR_MAX_CACHED_PROCESSES = "CUR_MAX_CACHED_PROCESSES";
    String mOverrideMaxCachedProcesses = "mOverrideMaxCachedProcesses";
    String mCustomizedMaxCachedProcesses = "mCustomizedMaxCachedProcesses";
    String DEFAULT_MAX_CACHED_PROCESSES = "DEFAULT_MAX_CACHED_PROCESSES";
    String MAX_CACHED_PROCESSES = "MAX_CACHED_PROCESSES";
    String CUR_TRIM_CACHED_PROCESSES = "CUR_TRIM_CACHED_PROCESSES";
    String KEY_MAX_CACHED_PROCESSES = "KEY_MAX_CACHED_PROCESSES";
    /**
     * {@link ClassConstants#DeviceConfig}
     */
    String NAMESPACE_ACTIVITY_MANAGER = "NAMESPACE_ACTIVITY_MANAGER";

    String mPackageManagerService = "mPackageManagerService";
    String mDefaultAppProvider = "mDefaultAppProvider";
    String mInjector = "mInjector";
    String SIGNAL_QUIT = "SIGNAL_QUIT";
    String SIGNAL_KILL = "SIGNAL_KILL";
    String SIGNAL_USR1 = "SIGNAL_USR1";
    String mOomAdjuster = "mOomAdjuster";
    String mCachedAppOptimizer = "mCachedAppOptimizer";
    String mThread = "mThread";
    String mPidsSelfLocked = "mPidsSelfLocked";
    String mProcLock = "mProcLock";

    String PROCESS_CLEANER_ENABLED = "PROCESS_CLEANER_ENABLED";
    String pgfault = "pgfault";
    String pgmajfault = "pgmajfault";
    String rssInBytes = "rssInBytes";
    String cacheInBytes = "cacheInBytes";
    String swapInBytes = "swapInBytes";
    String hasTopUi = "hasTopUi";
    String ACTIVITY_DESTROYED = "ACTIVITY_DESTROYED";
    String mDyingPid = "mDyingPid";
    String mMemFactorOverride = "mMemFactorOverride";
    String mPressureState = "mPressureState";
    String mAvailable = "mAvailable";
    String DEFAULT_USE_COMPACTION = "DEFAULT_USE_COMPACTION";
    String mFreezerDebounceTimeout = "mFreezerDebounceTimeout";
    String intent = "intent";
    String mUserId = "mUserId";
    String mWindowProcessController = "mWindowProcessController";
    String mHasClientActivities = "mHasClientActivities";
}
