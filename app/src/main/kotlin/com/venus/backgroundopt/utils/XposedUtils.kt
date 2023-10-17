package com.venus.backgroundopt.utils

import de.robv.android.xposed.XposedHelpers

/**
 * @author XingC
 * @date 2023/10/17
 */
object XposedUtils {
    @JvmStatic
    inline fun <reified E> getStaticObjectFieldValue(fieldName: String, defaultValue: Any?): Any? {
        return XposedHelpers.getStaticObjectField(E::class.java, fieldName) ?: defaultValue
    }

    @JvmStatic
    inline fun <reified E> getStaticStringFieldValue(
        fieldName: String,
        defaultValue: String?
    ): String? {
        return getStaticObjectFieldValue<E>(fieldName, defaultValue) as String?
    }

    @JvmStatic
    inline fun <reified E> getStaticIntFieldValue(fieldName: String): Int {
        return XposedHelpers.getStaticIntField(E::class.java, fieldName)
    }

    @JvmStatic
    inline fun <reified E> getStaticLongFieldValue(fieldName: String): Long {
        return XposedHelpers.getStaticLongField(E::class.java, fieldName)
    }

    @JvmStatic
    inline fun <reified E> getStaticDoubleFieldValue(fieldName: String): Double {
        return XposedHelpers.getStaticDoubleField(E::class.java, fieldName)
    }

    @JvmStatic
    inline fun <reified E> getStaticBooleanFieldValue(fieldName: String): Boolean {
        return XposedHelpers.getStaticBooleanField(E::class.java, fieldName)
    }
}