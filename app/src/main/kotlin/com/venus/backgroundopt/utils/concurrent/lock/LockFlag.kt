package com.venus.backgroundopt.utils.concurrent.lock

import java.util.concurrent.locks.Lock

/**
 * 普通锁标识
 *
 * @author XingC
 */
interface LockFlag {
    fun getLock(): Lock
}

inline fun LockFlag.lock(block: () -> Unit) {
    val lock = getLock()
    lock.lock()
    try {
        block()
    } finally {
        lock.unlock()
    }
}