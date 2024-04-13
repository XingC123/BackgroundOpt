package com.venus.backgroundopt.utils

import android.os.Build

/**
 * @author XingC
 * @date 2024/4/12
 */
object SystemUtils {
    @JvmField
    val androidVersionCode: Int = Build.VERSION.SDK_INT

    @JvmField
    val isUOrHigher: Boolean = androidVersionCode >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}