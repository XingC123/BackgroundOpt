package com.venus.backgroundopt.annotation

import kotlin.reflect.KClass

/**
 * 用以标识字段为某个安卓的对象中的字段
 *
 * @author XingC
 * @date 2024/3/1
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.SOURCE)
annotation class AndroidObjectField(
    val objectClassPath: String = "",
    val objectClazz: KClass<*> = Any::class,
    val fieldName: String = "",
)
