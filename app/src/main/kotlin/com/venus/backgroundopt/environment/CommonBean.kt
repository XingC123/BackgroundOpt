package com.venus.backgroundopt.environment

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author XingC
 * @date 2023/9/27
 */

val commonThreadPoolExecutor: ExecutorService = Executors.newFixedThreadPool(2)

inline fun newThreadTask(crossinline block: () -> Unit) {
    commonThreadPoolExecutor.execute {
        block()
    }
}
