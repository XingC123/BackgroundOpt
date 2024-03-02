package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#OomAdjuster}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class OomAdjuster {
    @AndroidObject(classPath = ClassConstants.OomAdjuster)
    private final Object oomAdjuster;

    @AndroidObject
    public Object getOomAdjuster() {
        return oomAdjuster;
    }

    private final CachedAppOptimizer cachedAppOptimizer;

    public CachedAppOptimizer getCachedAppOptimizer() {
        return cachedAppOptimizer;
    }

    public OomAdjuster(@AndroidObject Object oomAdjuster, ClassLoader classLoader) {
        this.oomAdjuster = oomAdjuster;

        cachedAppOptimizer = new CachedAppOptimizer(
                XposedHelpers.getObjectField(oomAdjuster, FieldConstants.mCachedAppOptimizer),
                classLoader
        );
    }
}
