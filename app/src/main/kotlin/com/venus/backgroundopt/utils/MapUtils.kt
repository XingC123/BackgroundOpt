package com.venus.backgroundopt.utils

import com.alibaba.fastjson2.JSON
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2023/9/28
 */

/**
 * 从给定map中填充当前map
 *
 * @param K key的类型
 * @param E value的类型
 * @param from 获取填充数据的map
 * @return 当前map
 */
inline fun <K, reified E> MutableMap<K, E>.fill(from: Map<K, *>): MutableMap<K, E> {
    from.forEach { (k, v) ->
        this[k] = JSON.parseObject(v.toString(), E::class.java)
    }
    return this
}

inline fun <reified E> convertValueToTargetType(
    map: Map<String, *>,
    enableConcurrent: Boolean = false
): MutableMap<String, E> {
    return if (enableConcurrent) {
        ConcurrentHashMap<String, E>().fill(map)
    } else {
        hashMapOf<String, E>().fill(map)
    }
}