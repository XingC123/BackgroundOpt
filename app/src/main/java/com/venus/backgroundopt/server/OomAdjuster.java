package com.venus.backgroundopt.server;

import com.venus.backgroundopt.hook.constants.FieldConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link com.venus.backgroundopt.hook.constants.ClassConstants#OomAdjuster}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class OomAdjuster {
    private final Object oomAdjuster;

    public Object getOomAdjuster() {
        return oomAdjuster;
    }

    private final CachedAppOptimizer cachedAppOptimizer;

    public CachedAppOptimizer getCachedAppOptimizer() {
        return cachedAppOptimizer;
    }

    public OomAdjuster(Object oomAdjuster) {
        this.oomAdjuster = oomAdjuster;

        cachedAppOptimizer = new CachedAppOptimizer(
                XposedHelpers.getObjectField(oomAdjuster, FieldConstants.mCachedAppOptimizer)
        );
    }
}
