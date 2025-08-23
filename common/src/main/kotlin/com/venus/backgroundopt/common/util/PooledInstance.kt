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

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 池化的实例
 *
 * 该类尝试缓存一定数量的[T], 以提高对象的复用能力
 *
 * @author XingC
 * @date 2024/8/7
 */
class PooledInstance<F, T> @JvmOverloads constructor(
    /* 填充物 */
    private val filler: F,
    /* 最大缓存数量 */
    val poolMaxSize: Int = 10,
    /* 每次填充时数量的上限 */
    val refillLimit: Int = poolMaxSize / 2,
    /* 实际对象的生成器 */
    private val instanceGenerator: (F?) -> T,
) {
    /*private val executor = ExecutorUtils.newFixedThreadPool(
        coreSize = 1,
        factoryName = "PooledInstancePool"
    )*/
    private val instanceCache = ConcurrentLinkedQueue<T>().apply {
        // 初始填充
        repeat(poolMaxSize) {
            add(instanceGenerator(filler))
        }
    }
    private val cacheSize = AtomicInteger(poolMaxSize)
    private val isWriting = AtomicBoolean(false)

    /**
     * 从[instanceCache]拿取[T], 并做初始化操作[initBlock]。[data]将作为参数传递给[instanceGenerator]
     */
    fun take(data: F? = null, initBlock: T.() -> Unit): T {
        val obj = instanceCache.poll()?.also { cacheSize.decrementAndGet() } ?: run {
            instanceGenerator(data).also {
                isWriting.compareAndSet(false, true).ifTrue {
                    try {
                        refillToCache()
                    } finally {
                        isWriting.set(false)
                    }
                }
            }
        }

        return obj.apply(initBlock)
    }

    /**
     * 回收[instance]到[instanceCache]
     */
    fun recycle(instance: T): Boolean {
        while (true) {
            val size = cacheSize.get()
            if (size >= poolMaxSize) return false
            if (cacheSize.compareAndSet(size, size + 1)) {
                instanceCache.offer(instance)
                return true
            }
        }
    }

    /**
     * 向[instanceCache]中添加[T]缓存
     */
    private fun refillToCache() {
        val size = cacheSize.get()
        if (size >= poolMaxSize) {
            return
        }

        val refillCount = refillLimit - size
        repeat(refillCount) {
            instanceCache.offer(instanceGenerator(filler))
        }
        cacheSize.addAndGet(refillCount)
    }
}