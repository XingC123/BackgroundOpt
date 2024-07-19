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

package com.venus.backgroundopt.xposed.util.preference
import com.venus.backgroundopt.common.util.convertValueToTargetType
import com.venus.backgroundopt.common.util.log.logError
import com.venus.backgroundopt.common.util.parseObject
import com.venus.backgroundopt.xposed.BuildConfig
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * @author XingC
 * @date 2023/9/18
 */
object PreferencesUtil {
    /**
     * 获取配置
     *
     * @param path
     * @return
     */
    fun getPref(path: String): XSharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, path)
        return if (pref.file.canRead()) pref else {
            XposedBridge.log("${BuildConfig.APPLICATION_ID}: 配置文件不可读(将使用默认配置)!文件: $path")
            null
        }
    }

    fun getString(path: String, key: String, defaultValue: String? = null): String? =
        getPref(path)?.getString(key, defaultValue) ?: defaultValue

    fun getBoolean(path: String, key: String, defaultValue: Boolean = false): Boolean =
        getPref(path)?.getBoolean(key, defaultValue) ?: defaultValue

    fun getInt(path: String, key: String, defaultValue: Int = Int.MIN_VALUE): Int =
        getPref(path)?.getInt(key, defaultValue) ?: defaultValue

    fun getFloat(path: String, key: String, defaultValue: Float = Float.MIN_VALUE): Float =
        getPref(path)?.getFloat(key, defaultValue) ?: defaultValue

    fun getLong(path: String, key: String, defaultValue: Long = Long.MIN_VALUE): Long =
        getPref(path)?.getLong(key, defaultValue) ?: defaultValue

    fun getStringSet(
        path: String,
        key: String,
        defaultValue: MutableSet<String>? = null
    ): MutableSet<String>? =
        getPref(path)?.getStringSet(key, defaultValue) ?: defaultValue

    fun <E> getObject(path: String, key: String, clazz: Class<E>, defaultValue: E?): E? {
        return getString(path, key)?.let { v ->
            try {
                v.parseObject(clazz)
            } catch (t: Throwable) {
                logError(logStr = "反序列化错误: v: ${v}, 类型: ${clazz.canonicalName}", t = t)
                defaultValue
            }
        } ?: defaultValue
    }

    inline fun <reified E> prefAll(path: String): MutableMap<String, E>? {
        val preferences = PreferencesUtil.getPref(path)
        preferences ?: return null

        return convertValueToTargetType(preferences.all, enableConcurrent = true)
    }
}