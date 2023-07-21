package com.venus.backgroundopt.hook.handle.android.entity;

/**
 * 封装了 {@link com.venus.backgroundopt.hook.constants.ClassConstants#ActivityManagerConstants}
 * @author XingC
 * @date 2023/7/21
 */
public class ActivityManagerConstants {
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
}
