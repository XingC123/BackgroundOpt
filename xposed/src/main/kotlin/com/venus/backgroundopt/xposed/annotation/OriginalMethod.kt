package com.venus.backgroundopt.xposed.annotation

import kotlin.reflect.KClass

/**
 * 用以标识某个方法为安卓某类中的方法
 *
 * @author XingC
 * @date 2024/7/13
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OriginalMethod(
    val methodName: String = "",
    val classPath: String = "",
    val clazz: KClass<*> = Any::class,
    val returnTypePath: String = "",
    val returnType: KClass<*> = Any::class,
    val since: Int = Int.MIN_VALUE,
)
