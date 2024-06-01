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

import com.venus.backgroundopt.annotation.AndroidMethod
import com.venus.backgroundopt.annotation.AndroidObjectField
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.utils.findClass
import com.venus.backgroundopt.utils.getStaticBooleanFieldValue
import com.venus.backgroundopt.utils.getStaticIntFieldValue
import com.venus.backgroundopt.utils.runCatchThrowable

/**
 * @author XingC
 * @date 2024/4/19
 */
object UserHandle {
    @JvmStatic
    val userHandleClazz by lazy {
        ClassConstants.UserHandle.findClass(
            classLoader = RunningInfo.getInstance().classLoader
        )
    }

    /** Range of uids allocated for a user */
    @JvmStatic
    @get:AndroidObjectField
    val PER_USER_RANGE: Int = userHandleClazz.getStaticIntFieldValue(
        fieldName = FieldConstants.PER_USER_RANGE
    )

    /** A user id constant to indicate the "system" user of the device */
    @JvmStatic
    @get:AndroidObjectField
    val USER_SYSTEM: Int = runCatchThrowable(defaultValue = 0) {
        userHandleClazz.getStaticIntFieldValue(fieldName = FieldConstants.USER_SYSTEM)
    }!!

    /**
     * Enable multi-user related side effects. Set this to false if
     * there are problems with single user use-cases
     */
    @JvmStatic
    @get:AndroidObjectField
    val MU_ENABLED: Boolean = userHandleClazz.getStaticBooleanFieldValue(
        fieldName = FieldConstants.MU_ENABLED
    )

    @JvmStatic
    @AndroidMethod
    fun getUserId(uid: Int): Int {
        return if (MU_ENABLED) {
            uid / PER_USER_RANGE
        } else {
            USER_SYSTEM
        }
    }
}