package com.venus.backgroundopt.utils

import java.util.stream.Stream

/**
 * @author XingC
 * @date 2023/9/28
 */

/* *************************************************************************
 *                                                                         *
 * Stream                                                                  *
 *                                                                         *
 **************************************************************************/
fun <E> Stream<E>.filterNullable(block: ((E) -> Boolean)? = null): Stream<E> {
    return block?.let { filter(block) } ?: this
}