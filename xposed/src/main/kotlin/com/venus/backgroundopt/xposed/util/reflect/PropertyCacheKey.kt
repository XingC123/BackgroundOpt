/*
 * Copyright (C) 2023-2025 BackgroundOpt
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

package com.venus.backgroundopt.xposed.util.reflect

/**
 * @author XingC
 * @date 2025/10/12
 */
abstract class PropertyCacheKey(
    val instanceClass: Class<*>,
    val propertyName: String,
)

/**
 * 方法缓存 key
 */
class MethodPropertyCacheKey(
    val paramTypes: Array<out Class<*>?>?,
    instanceClass: Class<*>,
    propertyName: String
) : PropertyCacheKey(
    instanceClass, propertyName
) {
    val hashCode  = computeHashCode(
        instanceClass = instanceClass,
        propertyName = propertyName,
        parameterTypes = paramTypes
    )

    private fun computeHashCode(
        instanceClass: Class<*>,
        propertyName: String,
        parameterTypes: Array<out Class<*>?>?
    ): Int {
        var result = instanceClass.hashCode()
        result = 31 * result + propertyName.hashCode()
        parameterTypes?.let { type ->
            result = 31 * result + type.contentHashCode()
        }
        return result
    }

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodPropertyCacheKey) return false

        return instanceClass == other.instanceClass &&
                propertyName == other.propertyName &&
                paramTypes.contentEquals(other.paramTypes)
    }

    override fun toString(): String {
        return instanceClass.getName() + '#' + propertyName + paramTypes.contentToString();
    }
}

/**
 * 字段缓存 key
 */
class FieldPropertyCacheKey(
    instanceClass: Class<*>,
    propertyName: String
) : PropertyCacheKey(
    instanceClass, propertyName
) {
    val hashCode = computeHashCode(
        instanceClass = instanceClass,
        propertyName = propertyName,
    )

    private fun computeHashCode(
        instanceClass: Class<*>,
        propertyName: String,
    ): Int {
        var result = instanceClass.hashCode()
        result = 31 * result + propertyName.hashCode()
        return result
    }

    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldPropertyCacheKey) return false

        return instanceClass == other.instanceClass &&
                propertyName == other.propertyName
    }

    override fun toString(): String {
        return instanceClass.getName() + '#' + propertyName;
    }
}
