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

package com.venus.backgroundopt.xposed.entity.base

import com.venus.backgroundopt.xposed.util.callStaticMethod
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KFunction

/**
 * 原生对象适配帮助者
 *
 * @author XingC
 * @date 2024/7/13
 */
interface IEntityCompatHelper<T> {
    val instanceClazz: Class<out T>
    val instanceCreator: (Any) -> T
}

inline fun <R> IEntityCompatHelper<*>.callStaticMethod(
    method: KFunction<R>,
    vararg params: Any?,
): R {
    return instanceClazz.callStaticMethod<R>(
        methodName = method.name,
        *params
    )
}

inline fun <R> IEntityCompatHelper<*>.callStaticMethod(methodName: String, vararg params: Any?): R {
    return instanceClazz.callStaticMethod<R>(
        methodName = methodName,
        *params
    )
}

/**
 * 不同版本的适配对象的伴生对象必须实现同一个接口A, 以提供相同的访问方式。而接口A则需要实现此接口
 */
interface IEntityCompatRule

/**
 * 如果某原生对象的包装类有分版本进行适配, 则此包装类应实现此接口
 */
interface IEntityCompatFlag

/**
 * 被注解的方法是原生实体的适配方法
 */
@Target(FUNCTION)
@Retention(BINARY)
annotation class IEntityCompatMethod
