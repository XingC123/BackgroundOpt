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
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessStateRecord.ProcessStateRecordHelper.getProcessStateRecordFromProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.ProcessStateRecordCompatSinceA12
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.ProcessStateRecordCompatUntilA11
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getObjectFieldValue

/**
 * @author XingC
 * @date 2024/7/14
 */
abstract class ProcessStateRecord(
    override var originalInstance: Any,
) : IEntityWrapper, IEntityCompatFlag {
    var maxAdj: Int
        get() = getMaxAdj(originalInstance)
        set(value) = setMaxAdj(originalInstance, value)

    var curAdj: Int
        get() = getCurAdj(originalInstance)
        set(value) = setCurAdj(originalInstance, value)

    var setAdj: Int
        get() = getSetAdj(originalInstance)
        set(value) = setSetAdj(originalInstance, value)

    var curRawAdj: Int
        get() = getCurRawAdj(originalInstance)
        set(value) = setCurRawAdj(originalInstance, value)

    var curProcState: Int
        get() = originalInstance.callMethod<Int>(MethodConstants.getCurProcState)
        set(value) = originalInstance.callMethod(MethodConstants.setCurProcState, value) as Unit

    var cached: Boolean
        get() = getCached(originalInstance)
        set(value) = setCached(originalInstance, value)

    var empty: Boolean
        get() = getEmpty(originalInstance)
        set(value) = setEmpty(originalInstance, value)

    val lastStateTime: Long get() = getLastStateTime(originalInstance)

    val hasForegroundActivities: Boolean
        get() = originalInstance.callMethod<Boolean>(
            MethodConstants.hasForegroundActivities
        )

    var curSchedGroup: Int
        get() = getCurSchedGroup(originalInstance)
        set(value) = setCurSchedGroup(originalInstance, value)

    var setSchedGroup: Int
        get() = getSetSchedGroup(originalInstance)
        set(value) = setSetSchedGroup(originalInstance, value)

    override fun init(instance: Any) {
        super.init(instance)

        this.originalInstance = getProcessStateRecordFromProcessRecord(instance)
    }

    // 经计算后的adj值
    // 未受进程 maxAdj影响的值
    var curComputedAdj: Int = ProcessList.UNKNOWN_ADJ

    object ProcessStateRecordHelper : IEntityCompatHelper<IProcessStateRecord, ProcessStateRecord> {
        override val instanceClazz: Class<out ProcessStateRecord>

        /**
         * 传入原生[ProcessRecord]
         */
        override val instanceCreator: (Any) -> ProcessStateRecord
        override val compatHelperInstance: IProcessStateRecord

        val getProcessStateRecordFromProcessRecord: (Any) -> Any

        init {
            if (OsUtils.isSOrHigher) {
                instanceClazz = ProcessStateRecordCompatSinceA12::class.java
                compatHelperInstance = ProcessStateRecordCompatSinceA12.Companion
                instanceCreator = ::createProcessStateRecordSinceA12
                getProcessStateRecordFromProcessRecord = ::getProcessStateRecordSinceA12
            } else {
                instanceClazz = ProcessStateRecordCompatUntilA11::class.java
                compatHelperInstance = ProcessStateRecordCompatUntilA11.Companion
                instanceCreator = ::createProcessStateRecordUntilA11
                getProcessStateRecordFromProcessRecord = ::getProcessStateRecordUntilA11
            }
        }

        private fun createProcessStateRecordSinceA12(@OriginalObject instance: Any): ProcessStateRecord {
            val psrInstance = getProcessStateRecordSinceA12(instance)
            return ProcessStateRecordCompatSinceA12(psrInstance)
        }

        private fun createProcessStateRecordUntilA11(@OriginalObject instance: Any): ProcessStateRecord =
            ProcessStateRecordCompatUntilA11(instance)

        private fun getProcessStateRecordSinceA12(@OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any): Any {
            return instance.getObjectFieldValue(FieldConstants.mState)!!
        }

        private fun getProcessStateRecordUntilA11(@OriginalObject(classPath = ClassConstants.ProcessRecord) instance: Any): Any {
            return instance
        }
    }

    companion object : IProcessStateRecord {
        @JvmStatic
        override fun getCurAdj(@OriginalObject instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getCurAdj(instance)
        }

        @JvmStatic
        override fun setCurAdj(@OriginalObject instance: Any, curAdj: Int) {
            ProcessStateRecordHelper.compatHelperInstance.setCurAdj(instance, curAdj)
        }

        @JvmStatic
        override fun getMaxAdj(instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getMaxAdj(instance)
        }

        @JvmStatic
        override fun setMaxAdj(instance: Any, maxAdj: Int) {
            ProcessStateRecordHelper.compatHelperInstance.setMaxAdj(instance, maxAdj)
        }

        @JvmStatic
        override fun getSetAdj(@OriginalObject instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getSetAdj(instance)
        }

        @JvmStatic
        override fun setSetAdj(@OriginalObject instance: Any, setAdj: Int) {
            ProcessStateRecordHelper.compatHelperInstance.setSetAdj(instance, setAdj)
        }

        @JvmStatic
        override fun getCurRawAdj(@OriginalObject instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getCurRawAdj(instance)
        }

        @JvmStatic
        override fun setCurRawAdj(@OriginalObject instance: Any, curRawAdj: Int) {
            ProcessStateRecordHelper.compatHelperInstance.setCurRawAdj(instance, curRawAdj)
        }

        @JvmStatic
        override fun getLastStateTime(@OriginalObject instance: Any): Long {
            return ProcessStateRecordHelper.compatHelperInstance.getLastStateTime(instance)
        }

        @JvmStatic
        override fun getCompletedAdjSeq(@OriginalObject instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getCompletedAdjSeq(instance)
        }

        @JvmStatic
        override fun getCached(instance: Any): Boolean {
            return ProcessStateRecordHelper.compatHelperInstance.getCached(instance)
        }

        @JvmStatic
        override fun setCached(instance: Any, cached: Boolean) {
            ProcessStateRecordHelper.compatHelperInstance.setCached(instance, cached)
        }

        @JvmStatic
        override fun getEmpty(instance: Any): Boolean {
            return ProcessStateRecordHelper.compatHelperInstance.getEmpty(instance)
        }

        @JvmStatic
        override fun setEmpty(instance: Any, empty: Boolean) {
            ProcessStateRecordHelper.compatHelperInstance.setEmpty(instance, empty)
        }

        @JvmStatic
        override fun getCurSchedGroup(instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getCurSchedGroup(instance)
        }

        @JvmStatic
        override fun setCurSchedGroup(instance: Any, curSchedGroup: Int) {
            ProcessStateRecordHelper.compatHelperInstance.setCurSchedGroup(instance, curSchedGroup)
        }

        @JvmStatic
        override fun getSetSchedGroup(instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getSetSchedGroup(instance)
        }

        @JvmStatic
        override fun setSetSchedGroup(instance: Any, setSchedGroup: Int) {
            ProcessStateRecordHelper.compatHelperInstance.setSetSchedGroup(instance, setSchedGroup)
        }

        @JvmStatic
        override fun getPid(instance: Any): Int {
            return ProcessStateRecordHelper.compatHelperInstance.getPid(instance)
        }
    }
}

interface IProcessStateRecord : IEntityCompatRule {
    fun getCurAdj(@OriginalObject instance: Any): Int
    fun setCurAdj(@OriginalObject instance: Any, curAdj: Int)

    fun getMaxAdj(@OriginalObject instance: Any): Int
    fun setMaxAdj(@OriginalObject instance: Any, maxAdj: Int)

    fun getSetAdj(@OriginalObject instance: Any): Int
    fun setSetAdj(@OriginalObject instance: Any, setAdj: Int)

    fun getCurRawAdj(@OriginalObject instance: Any): Int
    fun setCurRawAdj(@OriginalObject instance: Any, curRawAdj: Int)

    fun getLastStateTime(@OriginalObject instance: Any): Long
    fun getCompletedAdjSeq(@OriginalObject instance: Any): Int

    fun getCached(@OriginalObject instance: Any): Boolean
    fun setCached(@OriginalObject instance: Any, cached: Boolean)

    fun getEmpty(@OriginalObject instance: Any): Boolean
    fun setEmpty(@OriginalObject instance: Any, empty: Boolean)

    fun getCurSchedGroup(@OriginalObject instance: Any): Int
    fun setCurSchedGroup(@OriginalObject instance: Any, curSchedGroup: Int)

    fun getSetSchedGroup(@OriginalObject instance: Any): Int
    fun setSetSchedGroup(@OriginalObject instance: Any, setSchedGroup: Int)

    fun getPid(@OriginalObject instance: Any): Int
}