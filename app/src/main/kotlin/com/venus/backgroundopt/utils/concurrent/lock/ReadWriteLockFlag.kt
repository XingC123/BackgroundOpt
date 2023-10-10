package com.venus.backgroundopt.utils.concurrent.lock

import java.util.concurrent.locks.ReadWriteLock

/**
 * 读写锁标识
 *
 * @author XingC
 */
interface ReadWriteLockFlag {
    fun getReadWriteLock(): ReadWriteLock
}

inline fun ReadWriteLockFlag.lock(block: () -> Unit) {
    val readLock = getReadWriteLock().readLock()
    readLock.lock()
    try {
        block()
    } finally {
        readLock.unlock()
    }
}

inline fun ReadWriteLockFlag.writeLock(block: () -> Unit) {
    val writeLock = getReadWriteLock().writeLock()
    writeLock.lock()
    try {
        block()
    } finally {
        writeLock.unlock()
    }
}