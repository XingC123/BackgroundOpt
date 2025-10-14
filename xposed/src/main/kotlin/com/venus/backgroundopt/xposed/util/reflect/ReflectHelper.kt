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

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * 将Xposed的查找方法和字段的方法挪过来, 去除其缓存策略
 *
 * @author XingC
 * @date 2025/10/12
 */
object ReflectHelper {
    private val NO_SUCH_METHOD_METHOD = Method::class.java.getDeclaredConstructor().newInstance()

    private val NO_SUCH_FIELD_FIELD = Field::class.java.getDeclaredConstructor().newInstance()

    private val methodCache = ConcurrentHashMap<PropertyCacheKey, Method>()
    private val fieldCache = ConcurrentHashMap<PropertyCacheKey, Field>()

    /**
     * 精确查找方法（XposedHelpers.findMethodExact的提取版本）
     */
    fun findMethodExact(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method {
        return clazz.getDeclaredMethod(methodName, *parameterTypes).apply {
            isAccessible = true
        }
    }

    /**
     * 最佳匹配查找方法（XposedHelpers.findMethodBestMatch的提取版本）
     */
    fun findMethodBestMatch(
        instanceClass: Class<*>,
        methodName: String,
        vararg paramTypes: Class<*>
    ): Method {
        val cacheKey = MethodPropertyCacheKey(
            instanceClass = instanceClass,
            propertyName = methodName,
            paramTypes = paramTypes
        )

        val method = methodCache[cacheKey]
        method?.let {
            if (method === NO_SUCH_METHOD_METHOD) throw NoSuchMethodError(cacheKey.toString())
            return method
        }

        try {
            return findMethodExact(instanceClass, methodName, *paramTypes).also {
                methodCache[cacheKey] = it
            }
        } catch (ignored: NoSuchMethodException) {
        }

        var bestMatch: Method? = null
        var clz = instanceClass
        var considerPrivateMethods = true
        do {
            for (method in clz.getDeclaredMethods()) {
                // don't consider private methods of superclasses
                if (!considerPrivateMethods && Modifier.isPrivate(method.modifiers)) continue

                // compare name and parameters
                if (method.name == methodName && isAssignable(
                        parameterTypes = paramTypes,
                        methodParameterTypes = method.parameterTypes,
                        autoboxing = true
                    )
                ) {
                    // get accessible version of method
                    if (bestMatch == null || compareParameterTypes(
                            method.parameterTypes,
                            bestMatch.parameterTypes,
                            paramTypes
                        ) < 0
                    ) {
                        bestMatch = method
                    }
                }
            }
            considerPrivateMethods = false
        } while ((clz.getSuperclass().also { clz = it }) != null)

        if (bestMatch != null) {
            return bestMatch.apply {
                isAccessible = true
            }.also {
                methodCache[cacheKey] = it
            }
        } else {
            methodCache[cacheKey] = NO_SUCH_METHOD_METHOD
            throw NoSuchMethodError(cacheKey.toString())
        }
    }

    /**
     * 检查参数类型是否可赋值（来自Xposed的ClassUtils）
     */
    private fun isAssignable(
        parameterTypes: Array<out Class<*>?>,
        methodParameterTypes: Array<Class<*>>,
        autoboxing: Boolean
    ): Boolean {
        if (parameterTypes.size != methodParameterTypes.size) {
            return false
        }

        for (i in parameterTypes.indices) {
            val parameterType = parameterTypes[i]
            val methodParameterType = methodParameterTypes[i]

            if (parameterType != null) {
                if (autoboxing) {
                    if (methodParameterType.isPrimitive && parameterType == getWrapperClass(
                            methodParameterType
                        )
                    ) {
                        continue
                    }
                    if (parameterType.isPrimitive && methodParameterType == getWrapperClass(
                            parameterType
                        )
                    ) {
                        continue
                    }
                }

                if (!methodParameterType.isAssignableFrom(parameterType)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * 比较参数类型（来自Xposed的MemberUtils）
     */
    private fun compareParameterTypes(
        left: Array<Class<*>>,
        right: Array<Class<*>>,
        actual: Array<out Class<*>?>
    ): Int {
        val leftCost = getTotalTransformationCost(actual, left)
        val rightCost = getTotalTransformationCost(actual, right)
        return leftCost.compareTo(rightCost)
    }

    private fun getTotalTransformationCost(
        args: Array<out Class<*>?>,
        dest: Array<Class<*>>
    ): Float {
        var totalCost = 0.0f
        for (i in args.indices) {
            val srcClass = args[i]
            val destClass = dest[i]
            totalCost += getObjectTransformationCost(srcClass, destClass)
        }
        return totalCost
    }

    private fun getObjectTransformationCost(srcClass: Class<*>?, destClass: Class<*>): Float {
        if (destClass.isPrimitive) {
            return getPrimitivePromotionCost(srcClass, destClass)
        }

        var cost = 0.0f
        var src = srcClass
        while (src != null && !destClass.isAssignableFrom(src)) {
            if (destClass.isInterface && isAssignableFromInterface(src, destClass)) {
                cost += 0.25f
                break
            }
            cost++
            src = src.superclass
        }

        if (src == null) {
            cost += 1.5f
        }

        return cost
    }

    private fun getPrimitivePromotionCost(srcClass: Class<*>?, destClass: Class<*>): Float {
        var cost = 0.0f
        var cls = srcClass
        if (cls?.isPrimitive != true) {
            cost += 0.1f
            cls = getPrimitiveType(cls)
        }

        var i = 0
        while (cls != destClass) {
            when (cls) {
                Byte::class.javaPrimitiveType -> {
                    if (i > 0) return Float.MAX_VALUE
                    cls = Short::class.javaPrimitiveType
                }

                Short::class.javaPrimitiveType -> {
                    if (i > 1) return Float.MAX_VALUE
                    cls = Int::class.javaPrimitiveType
                }

                Char::class.javaPrimitiveType -> {
                    if (i > 1) return Float.MAX_VALUE
                    cls = Int::class.javaPrimitiveType
                }

                Int::class.javaPrimitiveType -> {
                    if (i > 2) return Float.MAX_VALUE
                    cls = Long::class.javaPrimitiveType
                }

                Long::class.javaPrimitiveType -> {
                    if (i > 3) return Float.MAX_VALUE
                    cls = Float::class.javaPrimitiveType
                }

                Float::class.javaPrimitiveType -> {
                    if (i > 4) return Float.MAX_VALUE
                    cls = Double::class.javaPrimitiveType
                }

                Double::class.javaPrimitiveType -> return Float.MAX_VALUE
            }
            i++
        }

        return cost + i
    }

    private fun isAssignableFromInterface(src: Class<*>, dest: Class<*>): Boolean {
        for (interfaceClass in src.interfaces) {
            if (interfaceClass == dest || isAssignableFromInterface(interfaceClass, dest)) {
                return true
            }
        }
        return false
    }

    private fun getPrimitiveType(cls: Class<*>?): Class<out Any>? {
        return when (cls) {
            java.lang.Byte::class.java -> Byte::class.javaPrimitiveType
            java.lang.Short::class.java -> Short::class.javaPrimitiveType
            java.lang.Character::class.java -> Char::class.javaPrimitiveType
            java.lang.Integer::class.java -> Int::class.javaPrimitiveType
            java.lang.Long::class.java -> Long::class.javaPrimitiveType
            java.lang.Float::class.java -> Float::class.javaPrimitiveType
            java.lang.Double::class.java -> Double::class.javaPrimitiveType
            java.lang.Boolean::class.java -> Boolean::class.javaPrimitiveType
            else -> cls
        }
    }

    private fun getWrapperClass(cls: Class<*>): Class<*> {
        return when (cls) {
            Byte::class.javaPrimitiveType -> java.lang.Byte::class.java
            Short::class.javaPrimitiveType -> java.lang.Short::class.java
            Char::class.javaPrimitiveType -> java.lang.Character::class.java
            Int::class.javaPrimitiveType -> java.lang.Integer::class.java
            Long::class.javaPrimitiveType -> java.lang.Long::class.java
            Float::class.javaPrimitiveType -> java.lang.Float::class.java
            Double::class.javaPrimitiveType -> java.lang.Double::class.java
            Boolean::class.javaPrimitiveType -> java.lang.Boolean::class.java
            else -> cls
        }
    }

    private fun getParametersString(parameterTypes: Array<out Class<*>>): String {
        val sb = StringBuilder("(")
        for (clazz in parameterTypes) {
            if (sb.length > 1) {
                sb.append(",")
            }
            sb.append(clazz?.canonicalName ?: "null")
        }
        sb.append(")")
        return sb.toString()
    }

    fun findField(
        instanceClass: Class<*>,
        fieldName: String
    ): Field {
        val cacheKey = FieldPropertyCacheKey(
            instanceClass = instanceClass,
            propertyName = fieldName
        )

        val field = fieldCache[cacheKey]
        field?.let {
            if (field === NO_SUCH_FIELD_FIELD) throw NoSuchFieldError(cacheKey.toString());
            return field
        }

        try {
            return findFieldRecursiveImpl(instanceClass, fieldName).apply {
                isAccessible = true
            }.also {
                fieldCache[cacheKey] = it
            }
        } catch (e: NoSuchFieldException) {
            fieldCache[cacheKey] = NO_SUCH_FIELD_FIELD
            throw NoSuchFieldError(cacheKey.toString())
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun findFieldRecursiveImpl(clazz: Class<*>?, fieldName: String): Field {
        var clazz = clazz
        try {
            return clazz!!.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            while (true) {
                clazz = clazz!!.getSuperclass()
                if (clazz == null || clazz == Any::class.java) break

                try {
                    return clazz.getDeclaredField(fieldName)
                } catch (ignored: NoSuchFieldException) {
                }
            }
            throw e
        }
    }
}