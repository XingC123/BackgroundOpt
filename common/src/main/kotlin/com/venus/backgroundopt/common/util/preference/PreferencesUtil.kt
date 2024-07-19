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

package com.venus.backgroundopt.common.util.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.common.util.convertValueToTargetType

/**
 * @author XingC
 * @date 2023/9/18
 */
class PreferencesUtil

@SuppressLint("WorldReadableFiles")
fun Context.pref(name: String): SharedPreferences =
    try {
        getSharedPreferences(name, Context.MODE_WORLD_READABLE)
    } catch (e: SecurityException) {
        getSharedPreferences(name, Context.MODE_PRIVATE)
    }

inline fun Context.prefEdit(
    name: String,
    commit: Boolean = false,
    action: SharedPreferences.Editor.() -> Unit
) {
    pref(name).edit(commit = commit, action = action)
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

fun Context.prefBoolean(
    name: String,
    key: String,
    defaultValue: Boolean = false
): Boolean {
    return pref(name).getBoolean(key, defaultValue)
}

fun Context.prefInt(
    name: String,
    key: String,
    defaultValue: Int = Int.MIN_VALUE
): Int {
    return pref(name).getInt(key, defaultValue)
}

fun Context.prefLong(
    name: String,
    key: String,
    defaultValue: Long = Long.MIN_VALUE
): Long {
    return pref(name).getLong(key, defaultValue)
}

fun Context.prefString(
    name: String,
    key: String,
    defaultValue: String? = null
): String? {
    return pref(name).getString(key, defaultValue)
}

fun SharedPreferences.Editor.putObject(key: String, value: Any) {
    putString(key, JSON.toJSONString(value))
}

