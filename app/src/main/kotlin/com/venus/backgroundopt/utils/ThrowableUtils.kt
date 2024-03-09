package com.venus.backgroundopt.utils

/**
 * @author XingC
 * @date 2024/2/26
 */

fun <R> runCatchThrowable(
    defaultValue: R? = null,
    catchBlock: ((Throwable) -> R)? = null,
    finallyBlock: (() -> Unit)? = null,
    block: () -> R
): R? {
    return try {
        block()
    } catch (t: Throwable) {
        catchBlock?.invoke(t) ?: defaultValue
    } finally {
        finallyBlock?.let { it() }
    }
}
