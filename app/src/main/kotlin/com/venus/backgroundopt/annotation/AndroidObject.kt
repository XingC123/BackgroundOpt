package com.venus.backgroundopt.annotation

import kotlin.reflect.KClass

/**
 * 如果某个变量是通过Xposed获取到的安卓源码中的对象, 则加上此注解加以标识
 *
 * @author XingC
 * @date 2024/3/1
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
)
@Retention(AnnotationRetention.SOURCE)
annotation class AndroidObject(val classPath: String = "", val clazz: KClass<*> = Any::class,)
