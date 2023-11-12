package com.venus.backgroundopt.utils.concurrent

import com.venus.backgroundopt.utils.concurrent.lock.LockFlag
import com.venus.backgroundopt.utils.concurrent.lock.ReadWriteLockFlag
import com.venus.backgroundopt.utils.concurrent.lock.lock
import com.venus.backgroundopt.utils.concurrent.lock.readLock
import com.venus.backgroundopt.utils.concurrent.lock.writeLock
import com.venus.backgroundopt.utils.log.logError
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author XingC
 * @date 2023/10/1
 */

object ConcurrentUtils {
    @JvmStatic
    @JvmOverloads
    fun execute(
        executorService: ExecutorService? = null,
        exceptionBlock: ((Throwable) -> Unit)? = null,
        block: () -> Unit
    ) {
        val service = executorService ?: commonThreadPoolExecutor
        service.execute {
            try {
                block()
            } catch (t: Throwable) {
                exceptionBlock?.let { it(t) } ?: run {
                    logError(
                        methodName = "ConcurrentUtils.execute",
                        logStr = "任务执行出错: ${t.message}",
                        t = t
                    )
                }
            }
        }
    }

    @JvmStatic
    fun executeResult(
        executorService: ExecutorService? = null,
        block: (ExecutorService) -> Unit
    ): Result<Unit> {
        return runCatching {
            val service = executorService ?: commonThreadPoolExecutor
            service.execute { block(service) }
        }
    }

    @JvmStatic
    fun executeResult(
        executorService: ExecutorService? = null,
        block: () -> Unit
    ): Result<Unit> {
        return executeResult(executorService) { _ ->
            block()
        }
    }
}

inline fun ExecutorService?.executeResult(crossinline block: (ExecutorService) -> Unit): Result<Unit> =
    ConcurrentUtils.executeResult(this) { executorService ->
        block(executorService)
    }

inline fun ExecutorService?.executeResult(crossinline block: () -> Unit): Result<Unit> =
    executeResult { _ -> block() }

/* *************************************************************************
 *                                                                         *
 * 全局线程池                                                                *
 *                                                                         *
 **************************************************************************/
val commonThreadPoolExecutor: ExecutorService = Executors.newFixedThreadPool(2)

inline fun newThreadTask(crossinline block: () -> Unit) {
    commonThreadPoolExecutor.execute {
        block()
    }
}

inline fun newThreadTaskResult(crossinline block: () -> Unit): Result<Unit> {
    return commonThreadPoolExecutor.executeResult(block)
}

/* *************************************************************************
 *                                                                         *
 * 锁                                                                      *
 *                                                                         *
 **************************************************************************/
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
 * 对应[LockFlag.lock]
 *
 * @param block 要执行的代码块
 */
inline fun LockFlag.lock(block: () -> Unit) = lock { block() }

/**
 * 对应[ReadWriteLockFlag.readLock]
 *
 * @param block 要执行的代码块
 */
inline fun ReadWriteLockFlag.readLock(block: () -> Unit) = readLock { block() }

/**
 * 对应[ReadWriteLockFlag.writeLock]
 *
 * @param block 要执行的代码块
 */
inline fun ReadWriteLockFlag.writeLock(block: () -> Unit) = writeLock { block() }
