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