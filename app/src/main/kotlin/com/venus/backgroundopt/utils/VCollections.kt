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
                    
 package com.venus.backgroundopt.utils

/**
 * @author XingC
 * @date 2023/8/20
 */
fun <T> supportRandomAccess(collection: Collection<T>): Boolean {
    return (collection is RandomAccess)
}

inline fun <T> Collection<T>.vFilter(predicate: (T) -> Boolean): List<T> {
    if (this is RandomAccess && this is List) {
        val list = arrayListOf<T>()
        var t: T
        for (i in this.indices) {
            t = this[i]
            if (predicate(t)) {
                list.add(t)
            }
        }
        return list
    }
    return this.filter { predicate(it) }
}

inline fun <T> Collection<T>.vForeach(action: (T) -> Unit) {
    if (this is RandomAccess && this is List) {
        for (i in this.indices) {
            action(this[i])
        }
        return
    }
    forEach { action(it) }
}