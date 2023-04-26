package com.venus.backgroundopt.utils;

import com.venus.backgroundopt.BuildConfig;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/26
 */
public class EnvironmentUtils {
    public static boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }
}
