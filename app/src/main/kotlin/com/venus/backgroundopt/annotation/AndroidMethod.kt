package com.venus.backgroundopt.annotation

import kotlin.reflect.KClass

/**
 * 用以标识某个方法为安卓某类中的方法
 *
 * @author XingC
 * @date 2024/3/1
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class AndroidMethod(
    val classPath: String = "",
    val clazz: KClass<*> = Any::class,
    val methodName: String = "",
)
