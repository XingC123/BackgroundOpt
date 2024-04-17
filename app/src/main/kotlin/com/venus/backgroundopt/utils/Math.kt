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

import kotlin.math.max
import kotlin.math.min

/**
 * @author XingC
 * @date 2024/4/17
 */

/**
 * 获取受区间约束的值
 *
 * 即: min <= x <= max
 * @param x Int 要判断的值
 * @param min Int 可取的最小值
 * @param max Int 可取的最大值
 * @return Int 最终的值
 */
fun clamp(x: Int, min: Int, max: Int): Int = min(max(x, min), max)

fun clamp(x: Long, min: Long, max: Long): Long = min(max(x, min), max)

fun clamp(x: Float, min: Float, max: Float): Float = min(max(x, min), max)

fun clamp(x: Double, min: Double, max: Double): Double = min(max(x, min), max)
