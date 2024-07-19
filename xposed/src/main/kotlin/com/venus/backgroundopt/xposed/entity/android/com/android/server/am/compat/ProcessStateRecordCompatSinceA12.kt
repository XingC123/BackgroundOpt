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
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.IProcessStateRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessStateRecord
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
class ProcessStateRecordCompatSinceA12(
    originalInstance: Any,
) : ProcessStateRecord(originalInstance) {
    companion object : IProcessStateRecord {
        @JvmStatic
        @IEntityCompatMethod
        override fun getCurAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getCurAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCurAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            curAdj: Int,
        ) {
            instance.callMethod(MethodConstants.setCurAdj, curAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getMaxAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getMaxAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setMaxAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            maxAdj: Int,
        ) {
            instance.callMethod(MethodConstants.setMaxAdj, maxAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getSetAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getSetAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setSetAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            setAdj: Int,
        ) {
            instance.callMethod(MethodConstants.setSetAdj, setAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCurRawAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getCurRawAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCurRawAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            curRawAdj: Int,
        ) {
            instance.callMethod(MethodConstants.setCurRawAdj, curRawAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getLastStateTime(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Long {
            return instance.callMethod<Long>(MethodConstants.getLastStateTime)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCompletedAdjSeq(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getCompletedAdjSeq)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCached(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Boolean {
            return instance.callMethod<Boolean>(MethodConstants.getCached)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCached(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            cached: Boolean,
        ) {
            instance.callMethod(MethodConstants.setCached, cached)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getEmpty(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Boolean {
            return instance.callMethod<Boolean>(MethodConstants.getEmpty)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setEmpty(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            empty: Boolean,
        ) {
            instance.callMethod(MethodConstants.setEmpty, empty)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCurSchedGroup(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getCurSchedGroup)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCurSchedGroup(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            curSchedGroup: Int,
        ) {
            instance.callMethod(
                MethodConstants.setCurSchedGroup,
                curSchedGroup
            )
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getSetSchedGroup(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.callMethod<Int>(MethodConstants.getSetSchedGroup)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setSetSchedGroup(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            setSchedGroup: Int,
        ) {
            instance.callMethod(
                MethodConstants.setSetSchedGroup,
                setSchedGroup
            )
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getPid(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return ProcessRecord.getPid(instance.getObjectFieldValue(FieldConstants.mApp)!!)
        }
    }
}