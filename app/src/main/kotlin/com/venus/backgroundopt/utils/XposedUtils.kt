package com.venus.backgroundopt.utils

import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.utils.log.logError
import com.venus.backgroundopt.utils.log.logInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * @author XingC
 * @date 2023/10/17
 */
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

fun Any.callMethod(methodName: String, paramTypes: Array<out Class<*>>, vararg args: Any?): Any? {
    return XposedHelpers.callMethod(this, methodName, paramTypes, *args)
}

fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? {
    return XposedHelpers.callStaticMethod(this, methodName, args)
}

fun Class<*>.callStaticMethod(
    methodName: String,
    paramTypes: Array<out Class<*>>,
    vararg args: Any?
): Any? {
    return XposedHelpers.callStaticMethod(this, methodName, paramTypes, args)
}

/* *************************************************************************
 *                                                                         *
 * hook                                                                    *
 *                                                                         *
 **************************************************************************/
private fun String.hookImpl(className: String, methodName: String?, block: () -> Unit) {
    try {
        block()
        if (!HookPoint.dontPrintLogHookList.contains("${className}.${methodName}")) {
            logInfo(logStr = "[${this}.${methodName}]hook成功")
        }
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
    vararg paramTypes: Any?
) {
    hookImpl(this, methodName) {
        XposedHelpers.findAndHookMethod(this, classLoader, methodName, *paramTypes, methodHook)
    }
}

fun String.hookAllMethods(
    classLoader: ClassLoader,
    methodHook: XC_MethodHook,
    methodName: String?,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
) {
    hookImpl(this, methodName) {
        val clazz = XposedHelpers.findClass(this, classLoader)
        if (methodType == HookPoint.MethodType.Constructor) {
            XposedBridge.hookAllConstructors(clazz, methodHook)
        } else {
            XposedBridge.hookAllMethods(
                XposedHelpers.findClass(this, classLoader),
                methodName,
                methodHook
            )
        }
    }
}

private fun String.hookMethod(
    classLoader: ClassLoader,
    methodName: String?,
    methodHook: XC_MethodHook,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?
) {
    if (hookAllMethod) {
        hookAllMethods(classLoader, methodHook, methodName, methodType)
    } else {
        normalHook(classLoader, methodName, methodHook, *paramTypes)
    }
}

fun String.beforeHook(
    classLoader: ClassLoader,
    methodName: String?,
    enable: Boolean = true,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
) {
    if (!enable) {
        return
    }
    hookMethod(classLoader, methodName, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
            block(param)
        }
    }, hookAllMethod, methodType, *paramTypes)
}

fun String.afterHook(
    classLoader: ClassLoader,
    methodName: String?,
    enable: Boolean = true,
    hookAllMethod: Boolean = false,
    methodType: HookPoint.MethodType = HookPoint.MethodType.Member,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
) {
    if (!enable) {
        return
    }
    hookMethod(classLoader, methodName, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            block(param)
        }
    }, hookAllMethod, methodType, *paramTypes)
}

fun String.beforeConstructorHook(
    classLoader: ClassLoader,
    enable: Boolean = true,
    hookAllMethod: Boolean = false,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
) {
    beforeHook(
        classLoader,
        null,
        enable,
        hookAllMethod,
        HookPoint.MethodType.Constructor,
        *paramTypes,
        block = block
    )
}

fun String.afterConstructorHook(
    classLoader: ClassLoader,
    enable: Boolean = true,
    hookAllMethod: Boolean = false,
    vararg paramTypes: Any?,
    block: (MethodHookParam) -> Unit
) {
    afterHook(
        classLoader,
        null,
        enable,
        hookAllMethod,
        HookPoint.MethodType.Constructor,
        *paramTypes,
        block = block
    )
}

@Suppress("UNCHECKED_CAST")
fun <E> String.findClass(classLoader: ClassLoader): Class<E> {
    return XposedHelpers.findClass(this, classLoader) as Class<E>
}
