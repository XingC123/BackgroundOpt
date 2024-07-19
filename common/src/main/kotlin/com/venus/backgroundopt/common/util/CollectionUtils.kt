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

package com.venus.backgroundopt.common.util

import java.util.stream.Stream

/**
 * @author XingC
 * @date 2023/9/28
 */
class CollectionUtils

/* *************************************************************************
 *                                                                         *
 * Stream                                                                  *
 *                                                                         *
 **************************************************************************/
fun <E> Stream<E>.nullableFilter(block: ((E) -> Boolean)? = null): Stream<E> {
    return block?.let { filter(block) } ?: this
}

/* *************************************************************************
 *                                                                         *
 * Sequence                                                                *
 *                                                                         *
 **************************************************************************/
fun <E> Sequence<E>.nullableFilter(block: ((E) -> Boolean)? = null): Sequence<E> {
    return block?.let { filter(block) } ?: this
}