package com.venus.backgroundopt.utils

import android.os.Build

/**
 * @author XingC
 * @date 2024/4/12
 */
object SystemUtils {
    @JvmStatic
    val androidVersionCode: Int
        get() {
            return Build.VERSION.SDK_INT
        }

    val isUOrHigher: Boolean
        get() {
            return androidVersionCode >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
}