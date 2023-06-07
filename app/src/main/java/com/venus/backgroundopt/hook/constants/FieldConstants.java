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
    /**
     *  {@link ClassConstants#ActivityManagerConstants}
     */
    String CUR_MAX_CACHED_PROCESSES = "CUR_MAX_CACHED_PROCESSES";
    String mOverrideMaxCachedProcesses = "mOverrideMaxCachedProcesses";
    String mCustomizedMaxCachedProcesses = "mCustomizedMaxCachedProcesses";
    String DEFAULT_MAX_CACHED_PROCESSES = "DEFAULT_MAX_CACHED_PROCESSES";
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
}
