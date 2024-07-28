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

import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.IProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getBooleanFieldValue
import com.venus.backgroundopt.xposed.util.getIntFieldValue
import com.venus.backgroundopt.xposed.util.getObjectFieldValue
import com.venus.backgroundopt.xposed.util.getStringFieldValue

/**
 * @author XingC
 * @date 2024/7/13
 */
class ProcessRecordCompatUntilA11(originalInstance: Any) : ProcessRecord(originalInstance) {
    companion object : IProcessRecord {
        @JvmStatic
        @IEntityCompatMethod
        override fun getPid(instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.pid)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getThread(instance: Any): Any? {
            return instance.getObjectFieldValue(FieldConstants.thread)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getIsolatedEntryPoint(instance: Any): String? {
            return instance.getStringFieldValue(
                FieldConstants.isolatedEntryPoint,
                defaultValue = null
            )
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun isKilledByAm(instance: Any): Boolean {
            return instance.getBooleanFieldValue(FieldConstants.killedByAm)
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
                methodName = MethodConstants.kill,
                reason,
                reasonCode,
                subReason,
                noisy
            )
        }
    }
}