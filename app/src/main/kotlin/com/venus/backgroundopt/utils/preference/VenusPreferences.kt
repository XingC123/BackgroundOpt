package com.venus.backgroundopt.utils.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.utils.convertValueToTargetType
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge


/**
 * @author XingC
 * @date 2023/9/18
 */

/**
 * 获取配置
 *
 * @param path
 * @return
 */
fun getPref(path: String): XSharedPreferences? {
    val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
    return if (pref.file.canRead()) pref else {
        XposedBridge.log("${BuildConfig.APPLICATION_ID}: 配置文件不可读!文件: $path")
        null
    }
}

fun getString(path: String, key: String): String? {
    val preferences = getPref(path)
    preferences ?: return null
    return preferences.getString(key, null)
}

fun getBoolean(path: String, key: String): Boolean {
    val preferences = getPref(path)
    preferences ?: return false
    return preferences.getBoolean(key, false)
}

fun getInt(path: String, key: String): Int {
    val preferences = getPref(path)
    preferences ?: return Int.MIN_VALUE
    return preferences.getInt(key, Int.MIN_VALUE)
}

fun getFloat(path: String, key: String): Float {
    val preferences = getPref(path)
    preferences ?: return Float.MIN_VALUE
    return preferences.getFloat(key, Float.MIN_VALUE)
}

fun getLong(path: String, key: String): Long {
    val preferences = getPref(path)
    preferences ?: return Long.MIN_VALUE
    return preferences.getLong(key, Long.MIN_VALUE)
}

fun getStringSet(path: String, key: String): MutableSet<String>? {
    val preferences = getPref(path)
    preferences ?: return null
    return preferences.getStringSet(key, null)
}

inline fun <reified E> prefAll(path: String): MutableMap<String, E>? {
    val preferences = getPref(path)
    preferences ?: return null

    return convertValueToTargetType(preferences.all, enableConcurrent = true)
}

/* *************************************************************************
 *                                                                         *
 * 安卓SharedPreference                                                     *
 *                                                                         *
 **************************************************************************/
fun Context.pref(name: String): SharedPreferences =
    try {
        getSharedPreferences(name, Context.MODE_WORLD_READABLE)
    } catch (e: SecurityException) {
        getSharedPreferences(name, Context.MODE_PRIVATE)
    }

fun Context.prefEdit(
    name: String,
    commit: Boolean = false,
    action: SharedPreferences.Editor.() -> Unit
) {
    pref(name).edit(commit, action)
}

fun Context.prefPut(
    name: String,
    commit: Boolean = false,
    key: String,
    value: Any
) {
    prefEdit(name, commit) {
        putString(key, JSON.toJSONString(value))
    }
}

inline fun <reified E> Context.prefValue(
    name: String,
    key: String
): E? {
    return pref(name).getString(key, null)?.let {
        JSON.parseObject(it, E::class.java)
    }
}

inline fun <reified E> Context.prefListValue(name: String, key: String): List<E>? {
    return pref(name).getString(key, null)?.let {
        JSON.parseArray(it, E::class.java)
    }
}

fun Context.prefAll(name: String): MutableMap<String, *> {
    return pref(name).all
}

@JvmName("prefAllWithType")
inline fun <reified E> Context.prefAll(name: String): MutableMap<String, E> {
    return convertValueToTargetType(pref(name).all)
}

