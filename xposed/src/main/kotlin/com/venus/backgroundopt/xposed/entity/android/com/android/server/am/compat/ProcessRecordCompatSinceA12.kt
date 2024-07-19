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
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.IProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getIntFieldValue

/**
 * @author XingC
 * @date 2024/7/13
 */
class ProcessRecordCompatSinceA12(originalInstance: Any) : ProcessRecord(originalInstance) {
    companion object : IProcessRecord {
        @JvmStatic
        @IEntityCompatMethod
        override fun getPid(@OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mPid)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getThread(@OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any): Any? {
            return instance.callMethod(MethodConstants.getThread)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getIsolatedEntryPoint(@OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any): String? {
            return instance.callMethod<String?>(MethodConstants.getIsolatedEntryPoint)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun isKilledByAm(instance: Any): Boolean {
            return instance.callMethod<Boolean>(MethodConstants.isKilledByAm)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun killLocked(
            instance: Any,
            reason: String,
            reasonCode: Int,
            subReason: Int,
            noisy: Boolean,
        ) {
            instance.callMethod(
                methodName = MethodConstants.killLocked,
                reason,
                reasonCode,
                subReason,
                noisy
            )
        }

    }
}