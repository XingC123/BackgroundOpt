/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.common.util.unsafeLazy
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.annotation.OriginalObjectField
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.CachedAppOptimizerA11
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.CachedAppOptimizerA12
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.util.reflect.findClass
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import kotlin.concurrent.Volatile

/**
 * @author XingC
 * @date 2024/10/28
 */
abstract class CachedAppOptimizer(
    @OriginalObject(classPath = ClassConstants.CachedAppOptimizer)
    override val originalInstance: Any,
) : IEntityWrapper, IEntityCompatFlag, ILogger {
    // Configured by phenotype. Updates from the server take effect immediately.
    var mCompactThrottleSomeSome: Long = DEFAULT_COMPACT_THROTTLE_1
    var mCompactThrottleSomeFull: Long = DEFAULT_COMPACT_THROTTLE_2
    var mCompactThrottleFullSome: Long = DEFAULT_COMPACT_THROTTLE_3
    var mCompactThrottleFullFull: Long = DEFAULT_COMPACT_THROTTLE_4

    @OriginalObjectField
    var mCompactThrottleMinOomAdj: Long = 0

    @OriginalObjectField
    var mCompactThrottleMaxOomAdj: Long = 0

    private val mFullCompactRequest: Long = 0

    @OriginalObjectField(
        objectClassPath = ClassConstants.CachedAppOptimizer,
        fieldName = FieldConstants.mFreezerDebounceTimeout
    )
    @Volatile
    var mFreezerDebounceTimeout: Long = DEFAULT_FREEZER_DEBOUNCE_TIMEOUT

    fun compactProcess(pid: Int, compactionFlags: Int): Boolean {
        return CachedAppOptimizerHelper.compatHelperInstance.compactProcess(
            originalInstance,
            pid,
            compactionFlags
        )
    }

    companion object {
        // Flags stored in the DeviceConfig API.
        const val KEY_USE_COMPACTION: String = "use_compaction"

        const val KEY_FREEZER_DEBOUNCE_TIMEOUT: String = "freeze_debounce_timeout"

        // Phenotype sends int configurations and we map them to the strings we'll use on device,
        // preventing a weird string value entering the kernel.
        const val COMPACT_ACTION_NONE: Int = 0
        const val COMPACT_ACTION_FILE: Int = 1
        const val COMPACT_ACTION_ANON: Int = 2
        const val COMPACT_ACTION_FULL: Int = 3

        // Handler constants.
        const val COMPACT_PROCESS_SOME: Int = 1
        const val COMPACT_PROCESS_FULL: Int = 2
        const val COMPACT_PROCESS_PERSISTENT: Int = 3
        const val COMPACT_PROCESS_BFGS: Int = 4
        const val COMPACT_PROCESS_MSG: Int = 1
        const val COMPACT_SYSTEM_MSG: Int = 2

        // Defaults for phenotype flags.
        const val DEFAULT_USE_COMPACTION: Boolean = true
        const val DEFAULT_USE_FREEZER: Boolean = true
        const val DEFAULT_COMPACT_THROTTLE_1: Long = 5_000
        const val DEFAULT_COMPACT_THROTTLE_2: Long = 10_000
        const val DEFAULT_COMPACT_THROTTLE_3: Long = 500
        const val DEFAULT_COMPACT_THROTTLE_4: Long = 10_000
        const val DEFAULT_COMPACT_THROTTLE_5: Long = (10 * 60 * 1000).toLong()
        const val DEFAULT_COMPACT_THROTTLE_6: Long = (10 * 60 * 1000).toLong()

        const val DEFAULT_FREEZER_DEBOUNCE_TIMEOUT: Long = 10000L

        val CachedAppOptimizerClass = unsafeLazy {
            ClassConstants.CachedAppOptimizer.findClass(RunningInfo.getInstance().classLoader)
        }

        // 进程压缩行为的锁
        @JvmStatic
        protected val processCompactLock = Any()

        private val compactActionArray = arrayOf(
            "all".toByteArray(StandardCharsets.UTF_8),
            "file".toByteArray(StandardCharsets.UTF_8),
        )

        fun compactProcessFs(pid: Int, compactionFlags: Int): Boolean {
            return runCatchThrowable(defaultValue = false) {
                FileOutputStream("/proc/${pid}/reclaim").use { fos ->
                    val index = when (compactionFlags) {
                        COMPACT_ACTION_FILE -> 1
                        else -> 0
                    }
                    val actionBytes = compactActionArray[index]
                    fos.write(actionBytes)
                }

                true
            }!!
        }

        // 安卓源码中的原方法
        //    void onOomAdjustChanged(int oldAdj, int newAdj, ProcessRecord app) {
        //        // Cancel any currently executing compactions
        //        // if the process moved out of cached state
        //        if (DefaultProcessDependencies.mPidCompacting == app.mPid && newAdj < oldAdj
        //                && newAdj < ProcessList.CACHED_APP_MIN_ADJ) {
        //            cancelCompaction();
        //        }
        //
        //        if (oldAdj <= ProcessList.PERCEPTIBLE_APP_ADJ
        //                && (newAdj == ProcessList.PREVIOUS_APP_ADJ || newAdj == ProcessList.HOME_APP_ADJ)) {
        //            // Perform a minor compaction when a perceptible app becomes the prev/home app
        //            // 当一个可感知的应用程序变成前一个/home程序时，执行一个小的压缩
        //            compactAppSome(app, false);
        //        } else if (oldAdj < ProcessList.CACHED_APP_MIN_ADJ
        //                && newAdj >= ProcessList.CACHED_APP_MIN_ADJ
        //                && newAdj <= ProcessList.CACHED_APP_MAX_ADJ) {
        //            // Perform a major compaction when any app enters cached
        //            // 可见, 当原oldAdj<缓存而newAdj>=缓存进程adj, 则进行全量压缩
        //            compactAppFull(app, false);
        //        }
        //    }
        fun isOomAdjEnteredCached(processRecord: ProcessRecord): Boolean {
            return isOomAdjEnteredCached(processRecord.getCurAdjNative())
        }

        fun isOomAdjEnteredCached(curAdj: Int): Boolean {
            return (curAdj >= ProcessList.CACHED_APP_MIN_ADJ && curAdj <= ProcessList.CACHED_APP_MAX_ADJ)
        }
    }
}

object CachedAppOptimizerHelper : IEntityCompatHelper<ICachedAppOptimizer, CachedAppOptimizer> {
    override val instanceClazz: Class<out CachedAppOptimizer>
    override val instanceCreator: (Any) -> CachedAppOptimizer
    override val compatHelperInstance: ICachedAppOptimizer

    init {
        if (OsUtils.isSOrHigher) {
            instanceClazz = CachedAppOptimizerA12::class.java
            instanceCreator = { CachedAppOptimizerA12(it) }
            compatHelperInstance = CachedAppOptimizerA12.Companion
        } else {
            instanceClazz = CachedAppOptimizerA11::class.java
            instanceCreator = { CachedAppOptimizerA11(it) }
            compatHelperInstance = CachedAppOptimizerA11.Companion
        }
    }

}

interface ICachedAppOptimizer : IEntityCompatRule {
    fun compactProcess(@OriginalObject instance: Any, pid: Int, compactionFlags: Int): Boolean
}