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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.wm

import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.util.getIntFieldValue
import com.venus.backgroundopt.xposed.util.getStringFieldValue

/**
 * @author XingC
 * @date 2024/7/11
 */
abstract class ActivityRecord(
    override val originalInstance: Any,
) : IEntityWrapper {
    companion object {
        @JvmStatic
        fun getUserId(@OriginalObject activity: Any): Int {
            return activity.getIntFieldValue(
                fieldName = FieldConstants.mUserId
            )
        }

        @JvmStatic
        fun getPackageName(@OriginalObject activity: Any): String {
            return activity.getStringFieldValue(
                fieldName = FieldConstants.packageName,
                defaultValue = ""
            )!!
        }
    }
}