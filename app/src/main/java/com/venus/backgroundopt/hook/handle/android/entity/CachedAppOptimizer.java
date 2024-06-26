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

package com.venus.backgroundopt.hook.handle.android.entity;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.annotation.AndroidObjectField;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.log.ILogger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#CachedAppOptimizer}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class CachedAppOptimizer implements ILogger, IAndroidEntity {
    // Flags stored in the DeviceConfig API.
    public static final String KEY_USE_COMPACTION = "use_compaction";

    public static final String KEY_FREEZER_DEBOUNCE_TIMEOUT = "freeze_debounce_timeout";

    // Phenotype sends int configurations and we map them to the strings we'll use on device,
    // preventing a weird string value entering the kernel.
    public static final int COMPACT_ACTION_NONE = 0;
    public static final int COMPACT_ACTION_FILE = 1;
    public static final int COMPACT_ACTION_ANON = 2;
    public static final int COMPACT_ACTION_FULL = 3;

    // Handler constants.
    public static final int COMPACT_PROCESS_SOME = 1;
    public static final int COMPACT_PROCESS_FULL = 2;
    public static final int COMPACT_PROCESS_PERSISTENT = 3;
    public static final int COMPACT_PROCESS_BFGS = 4;
    public static final int COMPACT_PROCESS_MSG = 1;
    public static final int COMPACT_SYSTEM_MSG = 2;

    // Defaults for phenotype flags.
    static final boolean DEFAULT_USE_COMPACTION = true;
    static final boolean DEFAULT_USE_FREEZER = true;
    static final long DEFAULT_COMPACT_THROTTLE_1 = 5_000;
    static final long DEFAULT_COMPACT_THROTTLE_2 = 10_000;
    static final long DEFAULT_COMPACT_THROTTLE_3 = 500;
    static final long DEFAULT_COMPACT_THROTTLE_4 = 10_000;
    static final long DEFAULT_COMPACT_THROTTLE_5 = 10 * 60 * 1000;
    static final long DEFAULT_COMPACT_THROTTLE_6 = 10 * 60 * 1000;

    public static final long DEFAULT_FREEZER_DEBOUNCE_TIMEOUT = 10_000L;

    // Configured by phenotype. Updates from the server take effect immediately.
    public long mCompactThrottleSomeSome = DEFAULT_COMPACT_THROTTLE_1;
    public long mCompactThrottleSomeFull = DEFAULT_COMPACT_THROTTLE_2;
    public long mCompactThrottleFullSome = DEFAULT_COMPACT_THROTTLE_3;
    public long mCompactThrottleFullFull = DEFAULT_COMPACT_THROTTLE_4;
    @AndroidObjectField
    public long mCompactThrottleMinOomAdj;
    @AndroidObjectField
    public long mCompactThrottleMaxOomAdj;

    private long mFullCompactRequest;

    @AndroidObjectField(
            objectClassPath = ClassConstants.CachedAppOptimizer,
            fieldName = FieldConstants.mFreezerDebounceTimeout
    )
    public volatile long mFreezerDebounceTimeout = DEFAULT_FREEZER_DEBOUNCE_TIMEOUT;

    @AndroidObject(classPath = ClassConstants.CachedAppOptimizer)
    private final Object cachedAppOptimizer;

    public CachedAppOptimizer(@AndroidObject Object cachedAppOptimizer, ClassLoader classLoader) {
        this.cachedAppOptimizer = cachedAppOptimizer;

        mCompactThrottleMinOomAdj =
                XposedHelpers.getLongField(cachedAppOptimizer, FieldConstants.mCompactThrottleMinOomAdj);
        mCompactThrottleMaxOomAdj =
                XposedHelpers.getLongField(cachedAppOptimizer, FieldConstants.mCompactThrottleMaxOomAdj);
    }

    @NonNull
    @Override
    @AndroidObject
    public Object getOriginalInstance() {
        return cachedAppOptimizer;
    }

    private Class<?> CachedAppOptimizerClass;

    public Class<?> getCachedAppOptimizerClass() {
        if (CachedAppOptimizerClass == null) {
//            CachedAppOptimizerClass = XposedHelpers.findClass(
//                    ClassConstants.CachedAppOptimizer,
//
//            )
            CachedAppOptimizerClass = cachedAppOptimizer.getClass();
        }
        return CachedAppOptimizerClass;
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

    public static boolean isOomAdjEnteredCached(ProcessRecord processRecord) {
        return isOomAdjEnteredCached(processRecord.getCurAdjNative());
    }

    public static boolean isOomAdjEnteredCached(int curAdj) {
        return (curAdj >= ProcessList.CACHED_APP_MIN_ADJ && curAdj <= ProcessList.CACHED_APP_MAX_ADJ);
    }

    /**
     * 全量压缩指定ProcessRecord
     * 该方法是对安卓源码对应方法的实现
     *
     * @param app   ProcessRecord
     * @param force 是否强制
     */
    public void compactAppFull(ProcessRecord app, boolean force) {
        boolean oomAdjEnteredCached = isOomAdjEnteredCached(app);

        ++mFullCompactRequest;
        XposedHelpers.setLongField(this.cachedAppOptimizer, FieldConstants.mFullCompactRequest, mFullCompactRequest);
        // Apply OOM adj score throttle for Full App Compaction.
        if (force || oomAdjEnteredCached) {
            app.getProcessCachedOptimizerRecord().setReqCompactAction(COMPACT_PROCESS_FULL);
            compactApp(app, force, "Full");
        }
    }

    public boolean compactApp(ProcessRecord app, boolean force, String compactRequestType) {
        return (boolean) XposedHelpers.callMethod(
                this.cachedAppOptimizer,
                MethodConstants.compactApp,
                app.getOriginalInstance(),
                force,
                compactRequestType
        );
    }

    /**
     * Compacts a process or app
     *
     * @param pid             pid of process to compact
     * @param compactionFlags selects the compaction type as defined by COMPACT_ACTION_{TYPE}_FLAG
     *                        constants
     * @return true if success, false if has problem
     */
    public boolean compactProcess(int pid, int compactionFlags) {
        try {
            XposedHelpers.callStaticMethod(
                    getCachedAppOptimizerClass(),
                    MethodConstants.compactProcess,
                    pid,
                    compactionFlags
            );
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 缓存的内存压缩的文件流 <br>
     * 进程的内存压缩不止一次执行, 缓存以提高性能 <br>
     * 初次使用时创建, 进程移除时进行关闭。
     */
    private final Map<Integer, FileOutputStream> compactOutputStreamMap = new ConcurrentHashMap<>();
    /**
     * 向内存压缩节点写入的值的字节流 <br>
     * 以便在使用时不需要重复性的进行 字符->字节数组 的转换
     */
    private final List<byte[]> compactActions = new ArrayList<>(2) {
        /**
         * 获取压缩行为的字节数组
         * @param action 压缩行为的具体str
         * @return 以UTF-8转换后的字节数组
         */
        private byte[] getActionBytes(String action) {
            return action.getBytes(StandardCharsets.UTF_8);
        }

        {
            // 使用的时候按照索引取出bytes[]
            add(getActionBytes("all"));
            add(getActionBytes("file"));
        }
    };

    /**
     * 移除缓存的内存压缩文件流
     * @param pid 缓存的文件流以pid为标识
     */
    public void removeCompactOutputStreams(int pid) {
        FileOutputStream fos = compactOutputStreamMap.remove(pid);
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * 强制内存压缩
     * @param pid   要压缩的进程的pid
     * @param compactionFlags 压缩的标识({@link #COMPACT_ACTION_FULL})
     * @return 写入了节点 -> true
     */
    public boolean compactProcessForce(int pid, int compactionFlags) {
        FileOutputStream fos = compactOutputStreamMap.computeIfAbsent(pid, key -> {
            try {
                String path = "/proc/" + pid + "/reclaim";
                return new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                return null;
            }
        });
        if (fos != null) {
            try {
                int index = switch (compactionFlags) {
                    case COMPACT_ACTION_FILE -> 1;
                    default -> 0;
                };
                byte[] actionBytes = compactActions.get(index);
                fos.write(actionBytes);
                return true;
            } catch (Throwable ignore) {
            }
        }
        return false;
    }
}