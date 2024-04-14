package com.venus.backgroundopt.utils

/**
 * @author XingC
 * @date 2024/4/15
 */
/* *************************************************************************
 *                                                                         *
 * Any                                                                     *
 *                                                                         *
 **************************************************************************/
inline fun Any?.ifNull(block: () -> Unit): Any? {
    this ?: run(block)
    return this
}

inline fun Any?.ifNotNull(block: (Any) -> Unit): Any? {
    this?.let { block(this) }
    return this
}

/* *************************************************************************
 *                                                                         *
 * Boolean                                                                 *
 *                                                                         *
 **************************************************************************/
inline fun Boolean.ifBlock(predicate: () -> Boolean, block: () -> Unit): Boolean {
    if (predicate()) {
        block()
    }

    return this
}

inline fun Boolean.ifTrue(block: () -> Unit): Boolean {
    return ifBlock(predicate = { this }, block = block)
}

inline fun Boolean.ifFalse(block: () -> Unit): Boolean {
    return ifBlock(predicate = { !this }, block = block)
}