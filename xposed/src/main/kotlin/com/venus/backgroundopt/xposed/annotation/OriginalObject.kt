package com.venus.backgroundopt.xposed.annotation

import kotlin.reflect.KClass

/**
 * 如果某个变量是通过Xposed获取到的原生对象, 则加上此注解加以标识
 *
 * @author XingC
 * @date 2024/7/13
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
)
@Retention(AnnotationRetention.SOURCE)
annotation class OriginalObject(
    val classPath: String = "",
    val clazz: KClass<*> = Any::class,
    val since: Int = Int.MIN_VALUE,
)
