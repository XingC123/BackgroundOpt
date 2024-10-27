/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.common.util

/**
 * @author XingC
 * @date 2024/10/26
 */
object KeyUtils {
    const val KEY_SEPARATOR = "#"

    @JvmStatic
    fun getAppKey(userId: Int, packageName: String): String {
        return if (userId == UserUtils.MAIN_USER) {
            packageName
        } else {
            "${userId}${KEY_SEPARATOR}${packageName}"
        }
    }

    @JvmStatic
    fun getAppKeyByUid(uid: Int, packageName: String): String {
        val userId = UserUtils.getUserId(uid)
        return if (userId == UserUtils.MAIN_USER) {
            packageName
        } else {
            "${userId}${KEY_SEPARATOR}${packageName}"
        }
    }

    @JvmStatic
    fun getProcessKey(userId: Int, processName: String): String {
        return getAppKey(userId, processName)
    }

    @JvmStatic
    fun getProcessKeyByUid(uid: Int, processName: String): String {
        val userId = UserUtils.getUserId(uid)
        return getProcessKey(userId, processName)
    }

    @JvmStatic
    fun getUserIdFromProcessKey(processKey: String): Int {
        val separatorIndex = processKey.lastIndexOf(KEY_SEPARATOR)
        return if (separatorIndex == -1) {
            UserUtils.MAIN_USER
        } else {
            processKey.substring(0, separatorIndex).toInt()
        }
    }
}