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
import com.venus.backgroundopt.xposed.util.getBooleanFieldValue
import com.venus.backgroundopt.xposed.util.getIntFieldValue
import com.venus.backgroundopt.xposed.util.getLongFieldValue
import com.venus.backgroundopt.xposed.util.getObjectFieldValue
import com.venus.backgroundopt.xposed.util.setBooleanFieldValue
import com.venus.backgroundopt.xposed.util.setIntFieldValue

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
            return instance.getIntFieldValue(FieldConstants.mCurAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCurAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            curAdj: Int,
        ) {
            instance.setIntFieldValue(FieldConstants.mCurAdj, curAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getMaxAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mMaxAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setMaxAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            maxAdj: Int,
        ) {
            instance.setIntFieldValue(FieldConstants.mMaxAdj, maxAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getSetAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mSetAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setSetAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            setAdj: Int,
        ) {
            instance.setIntFieldValue(FieldConstants.mSetAdj, setAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCurRawAdj(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mCurRawAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCurRawAdj(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            curRawAdj: Int,
        ) {
            instance.setIntFieldValue(FieldConstants.mCurRawAdj, curRawAdj)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getLastStateTime(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Long {
            return instance.getLongFieldValue(FieldConstants.mLastStateTime)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCompletedAdjSeq(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mCompletedAdjSeq)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCached(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Boolean {
            return instance.getBooleanFieldValue(FieldConstants.mCached)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCached(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            cached: Boolean,
        ) {
            instance.setBooleanFieldValue(FieldConstants.mCached, cached)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getEmpty(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Boolean {
            return instance.getBooleanFieldValue(FieldConstants.mEmpty)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setEmpty(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            empty: Boolean,
        ) {
            instance.setBooleanFieldValue(FieldConstants.mEmpty, empty)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getCurSchedGroup(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mCurSchedGroup)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setCurSchedGroup(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            curSchedGroup: Int,
        ) {
            instance.setIntFieldValue(FieldConstants.mCurSchedGroup, curSchedGroup)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getSetSchedGroup(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return instance.getIntFieldValue(FieldConstants.mSetSchedGroup)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun setSetSchedGroup(
            @OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any,
            setSchedGroup: Int,
        ) {
            instance.setIntFieldValue(FieldConstants.mSetSchedGroup, setSchedGroup)
        }

        @JvmStatic
        @IEntityCompatMethod
        override fun getPid(@OriginalObject(classPath = ClassConstants.ProcessStateRecord) instance: Any): Int {
            return ProcessRecord.getPid(instance.getObjectFieldValue(FieldConstants.mApp)!!)
        }
    }
}