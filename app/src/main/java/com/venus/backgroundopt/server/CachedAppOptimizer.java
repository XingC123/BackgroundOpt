package com.venus.backgroundopt.server;

import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link com.venus.backgroundopt.hook.constants.ClassConstants#CachedAppOptimizer}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class CachedAppOptimizer {
    // Phenotype sends int configurations and we map them to the strings we'll use on device,
    // preventing a weird string value entering the kernel.
    public static final int COMPACT_ACTION_NONE = 0;
    public static final int COMPACT_ACTION_FILE = 1;
    public static final int COMPACT_ACTION_ANON = 2;
    public static final int COMPACT_ACTION_FULL = 3;

    private final Object cachedAppOptimizer;

    public CachedAppOptimizer(Object cachedAppOptimizer) {
        this.cachedAppOptimizer = cachedAppOptimizer;
    }

    public Object getCachedAppOptimizer() {
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

    /**
     * Compacts a process or app
     *
     * @param pid             pid of process to compact
     * @param compactionFlags selects the compaction type as defined by COMPACT_ACTION_{TYPE}_FLAG
     *                        constants
     */
    public void compactProcess(int pid, int compactionFlags) {
        XposedHelpers.callStaticMethod(
                getCachedAppOptimizerClass(),
                MethodConstants.compactProcess,
                pid,
                compactionFlags
        );
    }
}
