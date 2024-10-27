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
 * @date 2024/8/30
 */
object UserUtils {
    const val MAIN_USER = 0
    const val USER_APP_UID_START_NUM = 10000
    const val PER_USER_RANGE = 100000

    @JvmStatic
    fun getUserId(uid: Int): Int {
        return if (uid > USER_APP_UID_START_NUM) {
            uid / PER_USER_RANGE
        } else {
            MAIN_USER
        }
    }
}
