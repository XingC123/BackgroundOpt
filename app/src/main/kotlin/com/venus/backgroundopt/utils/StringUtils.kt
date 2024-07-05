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

package com.venus.backgroundopt.utils

/**
 * @author XingC
 * @date 2023/9/29
 */

object StringUtils {
    /**
     * 判断给定字符串是否是空
     *
     * @param str
     * @return true if (str == null or str.isBlank())
     */
    fun isEmpty(str: String?): Boolean = str?.isBlank() ?: true
}

/**
 * 当前字符序列存在[other]则返回true(忽略类型)
 */
fun CharSequence.containsIgnoreCase(other: CharSequence): Boolean =
    this.contains(other, ignoreCase = true)

/**
 * 当前字符串与[other]相同则返回true(忽略类型)
 */
fun String?.equalsIgnoreCase(other: String?): Boolean = this.equals(other, ignoreCase = true)

