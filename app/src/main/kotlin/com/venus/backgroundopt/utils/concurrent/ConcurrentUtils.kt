package com.venus.backgroundopt.utils.concurrent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock

/**
 * @author XingC
 * @date 2023/10/1
 */

val synchronizedSet by lazy { ConcurrentHashSet<Any>() }
// ConcurrentHashMap<锁标识物, 锁对象>
// 不同锁标识物之间互不影响
//val lockMap by lazy { ConcurrentHashMap<Any, Any>() }

/**
 * 这只是保证一个代码块可以作为整体来运作。不会影响lock对象本身的锁
 * 由于先compute再remove, 在其间隙仍可能线程不安全。如果必须线程安全, 那就加锁
 * 注意: 传入的代码块中禁止再次使用同一个对象调用本方法, 即: any.lock{ any.lock{ // do something } } 这种写法是不允许的
 *      会直接抛出: java.lang.IllegalStateException: Recursive update
 * `    这是 [ConcurrentHashMap] 本身的特性!
 * @param lockFlag 竞争标识物
 * @param block 代码块
 */
@JvmName("lockGivenFlag")
inline fun lock(
    lockFlag: Any,
    crossinline block: () -> Unit
) {
    synchronizedSet.remove(
        synchronizedSet.add(lockFlag) {
            block()
        }
    )
}

/**
 * @see lock(lock: Any, block: () -> Unit)
 *
 * @param T 任意对象。如果直接调用, 则是使用当前实例对象
 * @param block 代码块
 */
inline fun <T : Any> T.lock(
    crossinline block: () -> Unit
) {
    lock(this, block)
}

/* *************************************************************************
 *                                                                         *
 * 读写锁                                                                   *
 *                                                                         *
 **************************************************************************/
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

/* *************************************************************************
 *                                                                         *
 * 可重入锁                                                                  *
 *                                                                         *
 **************************************************************************/
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

