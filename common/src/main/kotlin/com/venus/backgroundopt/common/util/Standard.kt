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

package com.venus.backgroundopt.common.util

/**
 * @author XingC
 * @date 2024/7/13
 */
class Standard

/* *************************************************************************
 *                                                                         *
 * Any                                                                     *
 *                                                                         *
 **************************************************************************/
inline fun <T> T?.ifNull(block: () -> Unit): T? {
    this ?: run(block)
    return this
}

inline fun <T> T?.ifNotNull(block: (T) -> Unit): T? {
    this?.let { block(this) }
    return this
}

inline fun <T> T?.eq(target: Any?, block: () -> Unit): T? {
    if (this == target) {
        block()
    }
    return this
}

inline fun <T> T?.ne(target: Any?, block: () -> Unit): T? {
    if (this != target) {
        block()
    }
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

/* *************************************************************************
 *                                                                         *
 * Number                                                                  *
 *                                                                         *
 **************************************************************************/
inline fun Int.le(target: Int, block: () -> Unit): Int {
    if (this <= target) {
        block()
    }
    return this
}

inline fun Int.lt(target: Int, block: () -> Unit): Int {
    if (this < target) {
        block()
    }
    return this
}

inline fun Int.ge(target: Int, block: () -> Unit): Int {
    if (this >= target) {
        block()
    }
    return this
}

inline fun Int.gt(target: Int, block: () -> Unit): Int {
    if (this > target) {
        block()
    }
    return this
}

inline fun Long.le(target: Long, block: () -> Unit): Long {
    if (this <= target) {
        block()
    }
    return this
}

inline fun Long.lt(target: Long, block: () -> Unit): Long {
    if (this < target) {
        block()
    }
    return this
}

inline fun Long.ge(target: Long, block: () -> Unit): Long {
    if (this >= target) {
        block()
    }
    return this
}

inline fun Long.gt(target: Long, block: () -> Unit): Long {
    if (this > target) {
        block()
    }
    return this
}

inline fun Float.le(target: Float, block: () -> Unit): Float {
    if (this <= target) {
        block()
    }
    return this
}

inline fun Float.lt(target: Float, block: () -> Unit): Float {
    if (this < target) {
        block()
    }
    return this
}

inline fun Float.ge(target: Float, block: () -> Unit): Float {
    if (this >= target) {
        block()
    }
    return this
}

inline fun Float.gt(target: Float, block: () -> Unit): Float {
    if (this > target) {
        block()
    }
    return this
}

inline fun Double.le(target: Double, block: () -> Unit): Double {
    if (this <= target) {
        block()
    }
    return this
}

inline fun Double.lt(target: Double, block: () -> Unit): Double {
    if (this < target) {
        block()
    }
    return this
}

inline fun Double.ge(target: Double, block: () -> Unit): Double {
    if (this >= target) {
        block()
    }
    return this
}

inline fun Double.gt(target: Double, block: () -> Unit): Double {
    if (this > target) {
        block()
    }
    return this
}
