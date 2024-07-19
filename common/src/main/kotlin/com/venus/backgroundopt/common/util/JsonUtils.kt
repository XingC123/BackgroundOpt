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

/**
 * @author XingC
 * @date 2023/11/8
 */
class JsonUtils

fun Any.toJsonString(): String = JSON.toJSONString(this)

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
