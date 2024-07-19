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

package com.venus.backgroundopt.xposed.util

import com.venus.backgroundopt.common.util.log.logError
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.IHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * @author XingC
 * @date 2023/10/17
 */
class XposedUtils

fun String.newInstance(classLoader: ClassLoader, vararg args: Any?): Any? {
    return XposedHelpers.findClass(this, classLoader)?.let { clazz ->
        XposedHelpers.newInstance(clazz, *args)
    }
}

fun String.newInstance(
    classLoader: ClassLoader,
    paramTypes: Array<out Class<*>>,
    vararg args: Any?
): Any? {
    return XposedHelpers.findClass(this, classLoader)?.let { clazz ->
        XposedHelpers.newInstance(clazz, paramTypes, *args)
    }
}

fun Class<*>.newInstanceXp(vararg args: Any?): Any {
    return XposedHelpers.newInstance(this, *args)
}

fun Class<*>.newInstanceXp(paramTypes: Array<out Class<*>>, vararg args: Any?): Any {
    return XposedHelpers.newInstance(this, paramTypes, *args)
}

/* *************************************************************************
 *                                                                         *
 * 获取/设置类字段值                                                          *
 *                                                                         *
 **************************************************************************/
fun Class<*>.getStaticObjectFieldValue(fieldName: String, defaultValue: Any? = null): Any? {
    return XposedHelpers.getStaticObjectField(this, fieldName) ?: defaultValue
}

fun Class<*>.setStaticObjectFieldValue(fieldName: String, value: Any?) {
    XposedHelpers.setStaticObjectField(this, fieldName, value)
}

fun Class<*>.getStaticStringFieldValue(fieldName: String, defaultValue: String?): String? {
    return getStaticObjectFieldValue(fieldName, defaultValue) as String?
}

fun Class<*>.setStaticStringFieldValue(fieldName: String, value: String?) {
    setObjectFieldValue(fieldName, value)
}

fun Class<*>.getStaticIntFieldValue(fieldName: String): Int {
    return XposedHelpers.getStaticIntField(this, fieldName)
}

fun Class<*>.setStaticIntFieldValue(fieldName: String, value: Int) {
    XposedHelpers.setStaticIntField(this, fieldName, value)
}

fun Class<*>.getStaticLongFieldValue(fieldName: String): Long {
    return XposedHelpers.getStaticLongField(this, fieldName)
}

fun Class<*>.setStaticLongFieldValue(fieldName: String, value: Long) {
    XposedHelpers.setStaticLongField(this, fieldName, value)
}

fun Class<*>.getStaticDoubleFieldValue(fieldName: String): Double {
    return XposedHelpers.getStaticDoubleField(this, fieldName)
}

fun Class<*>.setStaticDoubleFieldValue(fieldName: String, value: Double) {
    XposedHelpers.setStaticDoubleField(this, fieldName, value)
}

fun Class<*>.getStaticBooleanFieldValue(fieldName: String): Boolean {
    return XposedHelpers.getStaticBooleanField(this, fieldName)
}

fun Class<*>.setStaticBooleanFieldValue(fieldName: String, value: Boolean) {
    XposedHelpers.setStaticBooleanField(this, fieldName, value)
}

/* *************************************************************************
 *                                                                         *
 * 获取/设置对象字段值                                                         *
 *                                                                         *
 **************************************************************************/
fun Any.getObjectFieldValue(fieldName: String, defaultValue: Any? = null): Any? {
    return XposedHelpers.getObjectField(this, fieldName) ?: defaultValue
}

fun Any.setObjectFieldValue(fieldName: String, value: Any?) {
    XposedHelpers.setObjectField(this, fieldName, value)
}

fun Any.getStringFieldValue(fieldName: String, defaultValue: String?): String? {
    return getObjectFieldValue(fieldName, defaultValue) as String?
}

fun Any.setStringFieldValue(fieldName: String, value: String?) {
    setObjectFieldValue(fieldName, value)
}

fun Any.getIntFieldValue(fieldName: String): Int {
    return XposedHelpers.getIntField(this, fieldName)
}

fun Any.setIntFieldValue(fieldName: String, value: Int) {
    XposedHelpers.setIntField(this, fieldName, value)
}

fun Any.getLongFieldValue(fieldName: String): Long {
    return XposedHelpers.getLongField(this, fieldName)
}

fun Any.setLongFieldValue(fieldName: String, value: Long) {
    XposedHelpers.setLongField(this, fieldName, value)
}

fun Any.getDoubleFieldValue(fieldName: String): Double {
    return XposedHelpers.getDoubleField(this, fieldName)
}

fun Any.setDoubleFieldValue(fieldName: String, value: Double) {
    XposedHelpers.setDoubleField(this, fieldName, value)
}

fun Any.getBooleanFieldValue(fieldName: String): Boolean {
    return XposedHelpers.getBooleanField(this, fieldName)
}

fun Any.setBooleanFieldValue(fieldName: String, value: Boolean) {
    XposedHelpers.setBooleanField(this, fieldName, value)
}

/* *************************************************************************
 *                                                                         *
 * 方法调用                                                                 *
 *                                                                         *
 **************************************************************************/
fun Any.callMethod(methodName: String, vararg args: Any?): Any? {
    return XposedHelpers.callMethod(this, methodName, *args)
}

@JvmName("callMethodWithType")
@Suppress("UNCHECKED_CAST")
fun <R> Any.callMethod(methodName: String, vararg args: Any?): R {
    return callMethod(methodName = methodName, args = args) as R
}

fun Any.callMethod(methodName: String, paramTypes: Array<out Class<*>>, vararg args: Any?): Any? {
    return XposedHelpers.callMethod(this, methodName, paramTypes, *args)
}

@JvmName("callMethodWithType")
@Suppress("UNCHECKED_CAST")
fun <R> Any.callMethod(methodName: String, paramTypes: Array<out Class<*>>, vararg args: Any?): R {
    return callMethod(
        methodName = methodName,
        paramTypes = paramTypes,
        args = args
    ) as R
}

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? {
    return XposedHelpers.callStaticMethod(this, methodName, *args)
}

@JvmName("callStaticMethodWithType")
@Suppress("UNCHECKED_CAST")
fun <R> Class<*>.callStaticMethod(methodName: String, vararg args: Any?): R {
    return callStaticMethod(methodName = methodName, args = args) as R
}

fun Class<*>.callStaticMethod(
    methodName: String,
    paramTypes: Array<out Class<*>>,
    vararg args: Any?
): Any? {
    return XposedHelpers.callStaticMethod(this, methodName, paramTypes, *args)
}

@JvmName("callStaticMethodWithType")
@Suppress("UNCHECKED_CAST")
fun <R> Class<*>.callStaticMethod(
    methodName: String,
    paramTypes: Array<out Class<*>>,
    vararg args: Any?
): R {
    return callStaticMethod(
        methodName = methodName,
        paramTypes = paramTypes,
        args = args
    ) as R
}

/* *************************************************************************
 *                                                                         *
 * hook                                                                    *
 *                                                                         *
 **************************************************************************/
private fun String.hookImpl(className: String, methodName: String?, block: () -> Unit) {
    try {
        block()
        /*if (!HookPoint.dontPrintLogHookList.contains("${className}.${methodName}")) {
            logInfo(logStr = "[${this}.${methodName}]hook成功")
        }*/
    } catch (t: Throwable) {
        if (!HookPoint.dontPrintLogHookList.contains("${className}.${methodName}")) {
            logError(logStr = "[${this}.${methodName}]hook失败", t = t)
        }
    }
}

private fun String.normalHook(
    classLoader: ClassLoader,
    methodName: String?,
    methodHook: XC_MethodHook,
    tag: String? = null,
    vararg paramTypes: Any?
) {
    hookImpl(this, methodName) {
        val unhook = XposedHelpers.findAndHookMethod(
            this,
            classLoader,
            methodName,
            *paramTypes,
            methodHook
        )
        tag?.let {
            IHook.addMethodHookPoint(tag, unhook)
        }
    }
}

fun String.hookAllMethods(
    classLoader: ClassLoader,
    methodHook: XC_MethodHook,
    methodName: String?,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    tag: String? = null,
) {
    hookImpl(this, methodName) {
        val clazz = XposedHelpers.findClass(this, classLoader)
        val unhookSet = if (methodType == HookPoint.MethodType.Constructor) {
            XposedBridge.hookAllConstructors(clazz, methodHook)
        } else {
            XposedBridge.hookAllMethods(
                XposedHelpers.findClass(this, classLoader),
                methodName,
                methodHook
            )
        }
        tag?.let { IHook.addMethodHookPoint(tag, unhookSet) }
    }
}

private fun String.hookMethod(
    classLoader: ClassLoader,
    methodName: String?,
    methodHook: XC_MethodHook,
    tag: String? = null,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?
) {
    if (hookAllMethod) {
        hookAllMethods(
            classLoader = classLoader,
            methodHook = methodHook,
            methodName = methodName,
            methodType = methodType,
            tag = tag
        )
    } else {
        normalHook(
            classLoader = classLoader,
            methodName = methodName,
            methodHook = methodHook,
            tag = tag,
            paramTypes = paramTypes
        )
    }
}

fun printAfterAppearException(className: String, methodName: String?, throwable: Throwable) {
    logError(
        logStr = "beforeHook执行出错.className: ${className}, methodName: ${methodName}",
        t = throwable
    )
}

fun String.beforeHook(
    classLoader: ClassLoader,
    methodName: String?,
    enable: Boolean = true,
    tag: String? = null,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
): String {
    if (enable) {
        hookMethod(
            classLoader = classLoader,
            methodName = methodName,
            methodHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    super.beforeHookedMethod(param)
                    runCatchThrowable(catchBlock = {
                        printAfterAppearException(this@beforeHook, methodName, throwable = it)
                    }) {
                        block(param)
                    }
                }
            },
            tag = tag,
            hookAllMethod = hookAllMethod,
            methodType = methodType,
            paramTypes = paramTypes
        )
    }
    return this
}

fun String.afterHook(
    classLoader: ClassLoader,
    methodName: String?,
    enable: Boolean = true,
    tag: String? = null,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
): String {
    if (enable) {
        hookMethod(
            classLoader = classLoader,
            methodName = methodName,
            methodHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    runCatchThrowable(catchBlock = {
                        printAfterAppearException(this@afterHook, methodName, throwable = it)
                    }) {
                        block(param)
                    }
                }
            },
            tag = tag,
            hookAllMethod = hookAllMethod,
            methodType = methodType,
            paramTypes = paramTypes
        )
    }
    return this
}

fun String.replaceHook(
    classLoader: ClassLoader,
    methodName: String?,
    enable: Boolean = true,
    tag: String? = null,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Any?
): String {
    if (enable) {
        hookMethod(
            classLoader = classLoader,
            methodName = methodName,
            methodHook = object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    return runCatchThrowable(catchBlock = {
                        printAfterAppearException(this@replaceHook, methodName, throwable = it)
                    }) {
                        block(param)
                    }
                }
            },
            tag = tag,
            hookAllMethod = hookAllMethod,
            methodType = methodType,
            paramTypes = paramTypes
        )
    }
    return this
}

fun String.beforeConstructorHook(
    classLoader: ClassLoader,
    enable: Boolean = true,
    tag: String? = null,
    hookAllMethod: Boolean = false,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
): String {
    beforeHook(
        classLoader = classLoader,
        methodName = this.substring(this.lastIndexOf(".") + 1),
        enable = enable,
        tag = tag,
        hookAllMethod = hookAllMethod,
        methodType = HookPoint.MethodType.Constructor,
        paramTypes = paramTypes,
        block = block
    )
    return this
}

fun String.afterConstructorHook(
    classLoader: ClassLoader,
    enable: Boolean = true,
    tag: String? = null,
    hookAllMethod: Boolean = false,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
): String {
    afterHook(
        classLoader = classLoader,
        methodName = this.substring(this.lastIndexOf(".") + 1),
        enable = enable,
        tag = tag,
        hookAllMethod = hookAllMethod,
        methodType = HookPoint.MethodType.Constructor,
        paramTypes = paramTypes,
        block = block
    )
    return this
}

fun String.findClass(classLoader: ClassLoader): Class<*> {
    return XposedHelpers.findClass(this, classLoader)
}

fun String.findClassIfExists(classLoader: ClassLoader): Class<*>? {
    return XposedHelpers.findClassIfExists(this, classLoader)
}

@JvmName("findClassWithType")
@Suppress("UNCHECKED_CAST")
fun <E> String.findClass(classLoader: ClassLoader): Class<E> {
    return XposedHelpers.findClass(this, classLoader) as Class<E>
}