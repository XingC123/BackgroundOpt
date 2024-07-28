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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat

import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.IProcessServiceRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessServiceRecord
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getObjectFieldValue

/**
 * @author XingC
 * @date 2024/7/14
 */
class ProcessServiceRecordCompatUntilA11(
    originalInstance: Any,
) : ProcessServiceRecord(originalInstance) {
    companion object : IProcessServiceRecord {
        @JvmStatic
        @IEntityCompatMethod
        override fun numberOfRunningServices(@OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any): Int {
            return (instance.getObjectFieldValue(FieldConstants.mServices) as Set<*>).size
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun modifyRawOomAdj(
            @OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any,
            adj: Int,
        ): Int {
            return instance.callMethod<Int>(
                methodName = MethodConstants.modifyRawOomAdj,
                adj
            )
        }
    }
}