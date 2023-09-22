package com.venus.backgroundopt.utils.preference

import com.venus.backgroundopt.BuildConfig
import de.robv.android.xposed.XSharedPreferences


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
    return if (pref.file.canRead()) pref else null;
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
