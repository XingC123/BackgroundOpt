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
    String CUR_MAX_EMPTY_PROCESSES = "CUR_MAX_EMPTY_PROCESSES";
    String MAX_PHANTOM_PROCESSES = "MAX_PHANTOM_PROCESSES";
    String mOverrideMaxCachedProcesses = "mOverrideMaxCachedProcesses";
    String mCustomizedMaxCachedProcesses = "mCustomizedMaxCachedProcesses";
    String DEFAULT_MAX_CACHED_PROCESSES = "DEFAULT_MAX_CACHED_PROCESSES";
    String DEFAULT_MAX_PHANTOM_PROCESSES = "DEFAULT_MAX_PHANTOM_PROCESSES";
    String MAX_CACHED_PROCESSES = "MAX_CACHED_PROCESSES";
    String CUR_TRIM_CACHED_PROCESSES = "CUR_TRIM_CACHED_PROCESSES";
    String CUR_TRIM_EMPTY_PROCESSES = "CUR_TRIM_EMPTY_PROCESSES";
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
    String mReceivers = "mReceivers";
    String mOwner = "mOwner";
    String ASSISTANT = "ASSISTANT";
    String DEFAULT_INPUT_METHOD = "DEFAULT_INPUT_METHOD";
    String mAdjSeq = "mAdjSeq";
    String mServices = "mServices";
    String isolated = "isolated";
    String mAppProfiler = "mAppProfiler";
    String mService = "mService";
    String isSdkSandbox = "isSdkSandbox";
    String PER_USER_RANGE = "PER_USER_RANGE";
    String USER_SYSTEM = "USER_SYSTEM";
    String MU_ENABLED = "MU_ENABLED";
    String mConstants = "mConstants";
    String USE_TIERED_CACHED_ADJ = "USE_TIERED_CACHED_ADJ";
    String TIERED_CACHED_ADJ_DECAY_TIME = "TIERED_CACHED_ADJ_DECAY_TIME";
}