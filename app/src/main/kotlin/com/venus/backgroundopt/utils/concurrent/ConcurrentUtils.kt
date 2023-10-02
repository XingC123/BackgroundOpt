package com.venus.backgroundopt.utils.concurrent

/**
 * @author XingC
 * @date 2023/10/1
 */

val synchronizedSet by lazy { ConcurrentHashSet<Any>() }

/**
 * 这只是保证一个代码块可以作为整体来运作。不会影响lock对象本身的锁
 * 由于先compute再remove, 在其间隙仍旧可能线程不安全。如果必须线程安全, 那就加锁
 *
 * @param lock 竞争标识物
 * @param block 代码块
 */
@JvmName("visualSynchronizeWithLock")
inline fun visualSynchronize(
    lock: Any,
    crossinline block: () -> Unit
) {
    synchronizedSet.remove(
        synchronizedSet.add(lock) {
            block()
        }
    )
}

inline fun <T : Any> T.visualSynchronize(
    crossinline block: () -> Unit
) {
    visualSynchronize(this, block)
}
