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

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.filter.PropertyFilter


/**
 * @author XingC
 * @date 2023/11/8
 */
class JsonUtils

fun Any.toJsonString(): String {
    return JSON.toJSONString(this, object : PropertyFilter {
        override fun apply(obj: Any?, name: String?, value: Any?): Boolean {
            /*
             * 若将被序列化的类中有自引用(字段或方法)会导致栈溢出(循环引用问题暂不用考虑), 从而无法成功序列化。
             * 出问题的地方:
             * 序列化 android.content.pm.ApplicationInfo,
             * 其中有方法:
             * public ApplicationInfo getApplicationInfo() {
             *     return this;
             * }
             */
            return obj != value
        }
    })
}

fun <E> String.parseObject(clazz: Class<E>): E = JSON.parseObject(this, clazz)

fun <E> String.parseArray(clazz: Class<E>): MutableList<E> = JSON.parseArray(this, clazz)

/**
 * 将[JSONObject]转换为指定的类型[E]
 *
 * 若类class A { var value: Any? = null }导致fastjson无法正确映射value的类型。则使用本方法来进行转换
 */
inline fun <reified E> Any?.parseObjectFromJsonObject(): E? {
    return JSON.parseObject(this.toString(), E::class.java)
}
