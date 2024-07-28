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

package com.venus.backgroundopt.xposed.manager.process.oom

import com.venus.backgroundopt.xposed.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.self.AppInfo

/**
 * @author XingC
 * @date 2024/7/28
 */
abstract class OomAdjHandlerCommonMethods {
    protected open fun getAdjWillSet(processRecord: ProcessRecord, adj: Int): Int {
        throw UnsupportedOperationException()
    }

    /**
     * 计算adj
     */
    protected open fun computeAdj(processRecord: ProcessRecord, adj: Int): Int {
        throw UnsupportedOperationException()
    }

    protected open fun computeAdjByAdjHandleType(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean = adj >= 0,
        isHighPriorityProcess: Boolean = processRecord.isHighPriorityProcess(),
    ): Int {
        throw UnsupportedOperationException()
    }

    protected open fun doCustomMainProcessAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        throw UnsupportedOperationException()
    }

    protected open fun doCustomSubprocessAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
    ): Int {
        throw UnsupportedOperationException()
    }

    protected open fun doGlobalOomScoreAdj(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
        appInfo: AppInfo = processRecord.appInfo,
        appGroupEnum: AppGroupEnum = appInfo.appGroupEnum,
    ): Int {
        throw UnsupportedOperationException()
    }

    /**
     * 所有进程默认的处理方式
     */
    protected open fun doOther(
        processRecord: ProcessRecord,
        adj: Int,
        isUserSpaceAdj: Boolean,
        isHighPriorityProcess: Boolean,
        appInfo: AppInfo = processRecord.appInfo,
        appGroupEnum: AppGroupEnum = appInfo.appGroupEnum,
    ): Int {
        throw UnsupportedOperationException()
    }

    /**
     * 检查并对原生进程的adj设置最大值
     * @param processRecord ProcessRecord
     */
    protected open fun checkAndSetDefaultMaxAdjIfNeed(processRecord: ProcessRecord) {
        throw UnsupportedOperationException()
    }

    /**
     * 计算子进程的oom分数
     */
    protected open fun computeSubprocessAdj(
        processRecord: ProcessRecord,
        adj: Int,
    ): Int {
        throw UnsupportedOperationException()
    }

    /**
     * 计算高优先级进程可能会被使用的adj
     */
    protected open fun computeHighPriorityProcessPossibleAdj(
        processRecord: ProcessRecord,
        adj: Int,
        appInfo: AppInfo = processRecord.appInfo,
    ): Int {
        throw UnsupportedOperationException()
    }

    protected open fun computeMainProcessAdj(adj: Int): Int {
        throw UnsupportedOperationException()
    }

    protected open fun computeHighPrioritySubprocessAdj(adj: Int): Int {
        throw UnsupportedOperationException()
    }
}