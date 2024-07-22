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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.ProcessServiceRecordCompatSinceA12
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.ProcessServiceRecordCompatUntilA11
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.util.getObjectFieldValue

/**
 * 封装了[ClassConstants.ProcessServiceRecord]
 *
 * @author XingC
 * @date 2024/4/12
 */
abstract class ProcessServiceRecord(
    override val originalInstance: Any,
) : IEntityWrapper, IEntityCompatFlag {
    object ProcessServiceRecordHelper :
        IEntityCompatHelper<IProcessServiceRecord, ProcessServiceRecord> {
        override val instanceClazz: Class<out ProcessServiceRecord>
        override val instanceCreator: (Any) -> ProcessServiceRecord
        override val compatHelperInstance: IProcessServiceRecord

        val getProcessServiceRecordFromProcessRecord: (Any) -> Any

        init {
            if (OsUtils.isSOrHigher) {
                instanceClazz = ProcessServiceRecordCompatSinceA12::class.java
                compatHelperInstance = ProcessServiceRecordCompatSinceA12.Companion
                instanceCreator = ::createProcessServiceRecordSinceA12
                getProcessServiceRecordFromProcessRecord = ::getProcessServiceRecordSinceA12
            } else {
                instanceClazz = ProcessServiceRecordCompatUntilA11::class.java
                compatHelperInstance = ProcessServiceRecordCompatUntilA11.Companion
                instanceCreator = ::createProcessServiceRecordUntilA11
                getProcessServiceRecordFromProcessRecord = ::getProcessServiceRecordUntilA11
            }
        }

        private fun createProcessServiceRecordSinceA12(@OriginalObject instance: Any): ProcessServiceRecord {
            val psrInstance = getProcessServiceRecordSinceA12(instance)
            return ProcessServiceRecordCompatSinceA12(psrInstance)
        }

        private fun createProcessServiceRecordUntilA11(@OriginalObject instance: Any): ProcessServiceRecord =
            ProcessServiceRecordCompatUntilA11(instance)

        private fun getProcessServiceRecordSinceA12(@OriginalObject instance: Any): Any {
            return instance.getObjectFieldValue(FieldConstants.mServices)!!
        }

        private fun getProcessServiceRecordUntilA11(@OriginalObject instance: Any): Any {
            return instance
        }
    }

    companion object : IProcessServiceRecord {
        @JvmStatic
        override fun numberOfRunningServices(instance: Any): Int {
            return ProcessServiceRecordHelper.compatHelperInstance.numberOfRunningServices(instance)
        }

        @JvmStatic
        override fun modifyRawOomAdj(instance: Any, adj: Int): Int {
            return ProcessServiceRecordHelper.compatHelperInstance.modifyRawOomAdj(instance, adj)
        }
    }
}

interface IProcessServiceRecord : IEntityCompatRule {
    fun numberOfRunningServices(@OriginalObject instance: Any): Int

    fun modifyRawOomAdj(@OriginalObject instance: Any, adj: Int): Int
}
