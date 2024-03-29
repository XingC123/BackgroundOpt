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
                    
 package com.venus.backgroundopt.utils.log

import android.util.Log
import com.venus.backgroundopt.BuildConfig
import de.robv.android.xposed.XposedBridge

/**
 * @author XingC
 * @date 2023/9/20
 */

/**
 * 若处于debug模式, 则打印日志
 *
 * @param block
 */
inline fun printLogIfDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}

private fun logImpl(
    flag: String = "I",
    methodName: String = "",
    logStr: String,
    t: Throwable? = null
) {
    XposedBridge.log("Backgroundopt --> [${flag}]:${if (methodName == "") "" else " ${methodName}:"} $logStr")
    t?.let { XposedBridge.log(it) }
}

private fun logAndroidImpl(
    flag: String = "I",
    methodName: String = "",
    logStr: String,
    t: Throwable? = null
) {
    val finalLogStr = "${BuildConfig.APPLICATION_ID} --> [${flag}]: ${methodName}: $logStr"
    when (flag) {
        "I" -> {
            t?.let { Log.i(BuildConfig.APPLICATION_ID, finalLogStr, t) }
                ?: run {
                    Log.i(BuildConfig.APPLICATION_ID, finalLogStr)
                }
        }

        "D" -> {
            t?.let { Log.d(BuildConfig.APPLICATION_ID, finalLogStr, t) }
                ?: run {
                    Log.d(BuildConfig.APPLICATION_ID, finalLogStr)
                }
        }

        "W" -> {
            t?.let { Log.w(BuildConfig.APPLICATION_ID, finalLogStr, t) }
                ?: run {
                    Log.w(BuildConfig.APPLICATION_ID, finalLogStr)
                }
        }

        "E" -> {
            t?.let { Log.e(BuildConfig.APPLICATION_ID, finalLogStr, t) }
                ?: run {
                    Log.e(BuildConfig.APPLICATION_ID, finalLogStr)
                }
        }
    }
}

fun logInfo(logStr: String, methodName: String = "") {
    logImpl("I", methodName, logStr)
}

fun logInfoAndroid(logStr: String, methodName: String = "", t: Throwable? = null) {
    logAndroidImpl("I", methodName, logStr, t)
}

fun logDebug(logStr: String, methodName: String = "", t: Throwable? = null) {
    logImpl("D", methodName, logStr, t)
}

fun logDebugAndroid(logStr: String, methodName: String = "", t: Throwable? = null) {
    logAndroidImpl("D", methodName, logStr, t)
}

fun logWarn(logStr: String, methodName: String = "", t: Throwable? = null) {
    logImpl("W", methodName, logStr, t)
}

fun logWarnAndroid(logStr: String, methodName: String = "", t: Throwable? = null) {
    logAndroidImpl("W", methodName, logStr, t)
}

fun logError(logStr: String, methodName: String = "", t: Throwable? = null) {
    logImpl("E", methodName, logStr, t)
}

fun logErrorAndroid(logStr: String, methodName: String = "", t: Throwable? = null) {
    logAndroidImpl("E", methodName, logStr, t)
}