package com.venus.backgroundopt.utils.concurrent

/**
 * @author XingC
 * @date 2023/10/1
 */

val synchronizedSet by lazy { ConcurrentHashSet<Any>() }
// ConcurrentHashMap<锁标识物, 锁对象>
// 不同锁标识物之间互不影响
//val lockMap by lazy { ConcurrentHashMap<Any, Any>() }

/**
 * 当前换用synchronized锁
 *
 * <u>这只是保证一个代码块可以作为整体来运作。不会影响lock对象本身的锁</u>
 * <u>由于先compute再remove, 在其间隙仍旧可能线程不安全。如果必须线程安全, 那就加锁</u>
 *
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
//    synchronized(lockFlag) {
//        block()
//    }
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
