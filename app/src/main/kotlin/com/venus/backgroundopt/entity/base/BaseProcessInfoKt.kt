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

package com.venus.backgroundopt.entity.base

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.manager.process.AbstractAppOptimizeManager.AppOptimizeEnum
import com.venus.backgroundopt.manager.process.ProcessingResult
import com.venus.backgroundopt.utils.message.MessageFlag
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2023/9/26
 */
open class BaseProcessInfoKt(
    var uid: Int = Int.MIN_VALUE,
    @Volatile var pid: Int = Int.MIN_VALUE,
    var userId: Int = 0,
) : MessageFlag {
    /**
     * 主进程 -> 系统将要进行设置的oom_adj_score
     * 子进程 -> 真实oom_adj_score
     */
    var oomAdjScore: Int = Int.MIN_VALUE

    // 当前oom
    var curAdj: Int = Int.MIN_VALUE

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    @Volatile
    var fixedOomAdjScore = Int.MIN_VALUE

    // app主进程
    @Volatile
    var mainProcess = false

    lateinit var packageName: String
    lateinit var processName: String

    val lastProcessingResultMap =
        ConcurrentHashMap<AppOptimizeEnum, ProcessingResult>(AppOptimizeEnum.entries.size)

    // 是否是webview进程
    var webviewProcess = false
    var webviewProcessProbable = webviewProcess   /* 更宽泛的匹配条件 */

    fun initLastProcessingResultIfAbsent(
        appOptimizeEnum: AppOptimizeEnum,
        processingResultSupplier: () -> ProcessingResult
    ): ProcessingResult {
        return lastProcessingResultMap.computeIfAbsent(appOptimizeEnum) { _ ->
            processingResultSupplier()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseProcessInfoKt

        if (uid != other.uid) return false
        if (pid != other.pid) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid
        result = 31 * result + pid
        result = 31 * result + userId
        return result
    }
}