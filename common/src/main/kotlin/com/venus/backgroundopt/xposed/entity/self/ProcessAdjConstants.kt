package com.venus.backgroundopt.xposed.entity.self

/**
 * @author XingC
 * @date 2024/7/15
 */
object ProcessAdjConstants {
    /* *************************************************************************
     *                                                                         *
     * 来自原生对象: com.android.server.am.ProcessList                            *
     *                                                                         *
     **************************************************************************/
    const val INVALID_ADJ: Int = -10000

    const val UNKNOWN_ADJ: Int = 1001

    const val CACHED_APP_MAX_ADJ: Int = 999

    const val CACHED_APP_MIN_ADJ: Int = 900

    const val CACHED_APP_LMK_FIRST_ADJ: Int = 950

    const val CACHED_APP_IMPORTANCE_LEVELS: Int = 5

    const val SERVICE_B_ADJ: Int = 800

    const val PREVIOUS_APP_ADJ: Int = 700

    const val HOME_APP_ADJ: Int = 600

    const val SERVICE_ADJ: Int = 500

    const val HEAVY_WEIGHT_APP_ADJ: Int = 400

    const val BACKUP_APP_ADJ: Int = 300

    const val PERCEPTIBLE_LOW_APP_ADJ: Int = 250

    const val PERCEPTIBLE_MEDIUM_APP_ADJ: Int = 225

    const val PERCEPTIBLE_APP_ADJ: Int = 200

    const val VISIBLE_APP_ADJ: Int = 100

    const val VISIBLE_APP_LAYER_MAX: Int = PERCEPTIBLE_APP_ADJ - VISIBLE_APP_ADJ - 1

    const val PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ: Int = 50

    const val FOREGROUND_APP_ADJ: Int = 0

    const val PERSISTENT_SERVICE_ADJ: Int = -700

    const val PERSISTENT_PROC_ADJ: Int = -800

    const val SYSTEM_ADJ: Int = -900

    const val NATIVE_ADJ: Int = -1000

    /* *************************************************************************
     *                                                                         *
     * 模块定义                                                                  *
     *                                                                         *
     **************************************************************************/
    //不可能的adj取值
    const val IMPOSSIBLE_ADJ: Int = Int.MIN_VALUE

    const val MAX_ADJ: Int = 1000

    // 默认的主进程要设置的adj
    const val DEFAULT_MAIN_ADJ = FOREGROUND_APP_ADJ

    // 默认的子进程要设置的adj
    const val SUB_PROC_ADJ = VISIBLE_APP_ADJ + 1

    @JvmStatic
    fun isValidAdj(adj: Int): Boolean = adj in NATIVE_ADJ..<UNKNOWN_ADJ
}