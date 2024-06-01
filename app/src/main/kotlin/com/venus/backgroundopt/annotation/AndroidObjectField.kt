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
                    
 package com.venus.backgroundopt.annotation

import android.os.Build
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
    val since: Int = Build.VERSION_CODES.S,
)