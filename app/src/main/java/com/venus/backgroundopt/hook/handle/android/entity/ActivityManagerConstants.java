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

import android.os.Build;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.annotation.AndroidObjectField;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.utils.SystemUtils;
import com.venus.backgroundopt.utils.XposedUtilsKt;
import com.venus.backgroundopt.utils.log.ILogger;

/**
 * 封装了 {@link ClassConstants#ActivityManagerConstants}
 *
 * @author XingC
 * @date 2023/7/21
 */
public class ActivityManagerConstants implements IAndroidEntity, ILogger {
    public static final String KEY_USE_TIERED_CACHED_ADJ = "use_tiered_cached_adj";
    public static final String KEY_TIERED_CACHED_ADJ_DECAY_TIME = "tiered_cached_adj_decay_time";
    public static final String KEY_USE_MODERN_TRIM = "use_modern_trim";

    /**
     * 退到一定时间, 允许使用的最大cpu占比
     */
    // <=5min
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_1 = 25;
    // (5, 10]
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_2 = 25;
    // (10, 15]
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_3 = 10;
    // >15
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_4 = 2;

    public static final String KEY_MAX_CACHED_PROCESSES = "max_cached_processes";

    @AndroidObject(classPath = ClassConstants.ActivityManagerConstants)
    Object originalInstance;

    @NonNull
    @Override
    public Object getOriginalInstance() {
        return originalInstance;
    }

    public ActivityManagerConstants(@NonNull @AndroidObject Object originalInstance) {
        this.originalInstance = originalInstance;

        init(originalInstance);
    }

    private void init(@NonNull @AndroidObject Object originalInstance) {
        initIfIsUOrHigher(originalInstance);
    }

    private void initIfIsUOrHigher(@NonNull @AndroidObject Object originalInstance) {
        if (!SystemUtils.isUOrHigher) {
            return;
        }

        XposedUtilsKt.setBooleanFieldValue(
                originalInstance,
                FieldConstants.USE_TIERED_CACHED_ADJ,
                true
        );
        boolean USE_TIERED_CACHED_ADJ = XposedUtilsKt.getBooleanFieldValue(
                originalInstance,
                FieldConstants.USE_TIERED_CACHED_ADJ
        );
        if (BuildConfig.DEBUG) {
            getLogger().info("系统分层缓存: " + USE_TIERED_CACHED_ADJ);
        }
    }

    // 原字段为实例属性
    @AndroidObjectField(since = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static boolean USE_TIERED_CACHED_ADJ = false;

    // 原字段为实例属性
    @AndroidObjectField(since = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public static long TIERED_CACHED_ADJ_DECAY_TIME = 60 * 1000;
}