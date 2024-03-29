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
                    
 package com.venus.backgroundopt.utils.concurrent

import java.util.concurrent.ConcurrentHashMap

/**
 * 线程安全Set
 * 主要是[add] (obj: K, compute: (K) -> Unit), 利用ConcurrentHashMap来完成锁
 *
 * @author XingC
 * @date 2023/10/1
 */
class ConcurrentHashSet<K : Any> : MutableSet<K> {
    companion object {
        val any by lazy { Any() }
    }

    private val map by lazy {
        ConcurrentHashMap<K, Any>()
    }

    override fun add(element: K): Boolean {
        return map.put(element, any) == null
    }

    /**
     * 这是本类的重点。通过ConcurrentHashMap自身机制完成代码块级锁的效果
     *
     * @param element 要添加的元素
     * @param compute 要进行计算的方法
     * @return 添加的元素
     */
    fun add(element: K, compute: () -> Unit):K {
        map.compute(element) { _, _ ->
            compute()
            any
        }
        return element
    }

    override fun addAll(elements: Collection<K>): Boolean {
        return map.keys.addAll(elements)
    }

    override fun contains(element: K): Boolean {
        return map.keys.contains(element)
    }

    override fun containsAll(elements: Collection<K>): Boolean {
        return map.keys.containsAll(elements)
    }

    override val size: Int
        get() = map.keys.size

    override fun clear() {
        map.clear()
    }

    override fun isEmpty(): Boolean {
        return map.keys.isEmpty()
    }

    override fun iterator(): MutableIterator<K> {
        return map.keys.iterator()
    }

    override fun retainAll(elements: Collection<K>): Boolean {
        return map.keys.retainAll(elements.toSet())
    }

    override fun removeAll(elements: Collection<K>): Boolean {
        return map.keys.removeAll(elements.toSet())
    }

    override fun remove(element: K): Boolean {
        return map.remove(element) == null
    }

    override fun toString(): String {
        return map.keys.toString()
    }
}

