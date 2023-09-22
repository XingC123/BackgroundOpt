package com.venus.backgroundopt.utils.log

import com.venus.backgroundopt.BuildConfig

/**
 * @author XingC
 * @date 2023/9/20
 */

/**
 * 若处于debug模式, 则打印日志
 *
 * @param block
 */
inline fun printLogIfDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}