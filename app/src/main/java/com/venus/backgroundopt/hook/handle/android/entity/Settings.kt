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

package com.venus.backgroundopt.hook.handle.android.entity

import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.utils.getStaticStringFieldValue

/**
 * @author XingC
 * @date 2024/3/27
 */
class Settings {
    class Secure {
        companion object {
            @JvmField
            val ASSISTANT = android.provider.Settings.Secure::class.java.getStaticStringFieldValue(
                fieldName = FieldConstants.ASSISTANT,
                defaultValue = "-1"
            )

            @JvmField
            val DEFAULT_INPUT_METHOD =
                android.provider.Settings.Secure::class.java.getStaticStringFieldValue(
                    fieldName = FieldConstants.DEFAULT_INPUT_METHOD,
                    defaultValue = "-1"
                )
        }
    }
}