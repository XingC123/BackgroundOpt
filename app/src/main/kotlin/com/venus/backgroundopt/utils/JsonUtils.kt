package com.venus.backgroundopt.utils

import com.alibaba.fastjson2.JSON

/**
 * @author XingC
 * @date 2023/11/8
 */
object JsonUtils {
    @JvmStatic
    fun toJsonString(any: Any): String {
        return JSON.toJSONString(any)
    }

    @JvmStatic
    fun <E> parseObject(string: String, clazz: Class<E>): E {
        return JSON.parseObject(string, clazz)
    }

    @JvmStatic
    fun <E> parseArray(string: String, clazz: Class<E>): MutableList<E> {
        return JSON.parseArray(string, clazz)
    }
}