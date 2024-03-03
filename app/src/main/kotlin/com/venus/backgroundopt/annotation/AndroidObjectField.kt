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
    // 所属对象的类型的完整路径
    val objectClassPath: String = "",
    // 所属对象的类型
    val objectClazz: KClass<*> = Any::class,
    val fieldName: String = "",
    // 字段的类型的完整类路径
    val fieldTypeClassPath: String = "",
    // 字段的类型
    val fieldTypeClazz: KClass<*> = Any::class,
)
