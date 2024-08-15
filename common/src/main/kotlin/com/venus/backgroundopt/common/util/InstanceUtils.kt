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
 * @date 2024/8/15
 */
class InstanceUtils

/**
 * 获取或利用[createBlock]创建[T], 并在应用[initBlock]后返回
 */
inline fun <T> T?.getOrCreateThenInit(createBlock: () -> T, initBlock: T.() -> Unit): T {
    return (this ?: createBlock()).apply(initBlock)
}

/**
 * 当前对象不为空则直接返回; 空则利用[checkRadix]创建[T], 并在应用[initBlock]后返回
 */
inline fun <T> T?.getOrCreate(createBlock: () -> T, initBlock: T.() -> Unit): T {
    return this ?: (createBlock().apply(initBlock))
}