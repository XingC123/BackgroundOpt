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

import android.os.Build

/**
 * @author XingC
 * @date 2024/4/12
 */
object OsUtils {
    @JvmField
    val androidVersionCode: Int = Build.VERSION.SDK_INT

    const val R = Build.VERSION_CODES.R
    const val S = Build.VERSION_CODES.S
    const val T = Build.VERSION_CODES.TIRAMISU
    const val U = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @JvmField
    val isR: Boolean = androidVersionCode == Build.VERSION_CODES.R

    @JvmField
    val isROrHigher: Boolean = androidVersionCode >= Build.VERSION_CODES.R

    @JvmField
    val isSOrHigher: Boolean = androidVersionCode >= Build.VERSION_CODES.S

    @JvmField
    val isUOrHigher: Boolean = androidVersionCode >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}