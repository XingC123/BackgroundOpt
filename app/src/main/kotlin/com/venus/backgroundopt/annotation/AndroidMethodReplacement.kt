package com.venus.backgroundopt.annotation

import android.os.Build
import kotlin.reflect.KClass

/**
 * 标识对安卓方法的替换
 *
 * @author XingC
 * @date 2024/4/12
 */
annotation class AndroidMethodReplacement(
    val classPath: String = "",
    val clazz: KClass<*> = Any::class,
    val methodName: String = "",
    val since: Int = Build.VERSION_CODES.S,
)
