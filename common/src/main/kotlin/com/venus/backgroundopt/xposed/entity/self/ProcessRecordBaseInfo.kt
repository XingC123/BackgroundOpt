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

package com.venus.backgroundopt.xposed.entity.self

import com.venus.backgroundopt.common.util.message.MessageFlag
import com.venus.backgroundopt.xposed.manager.process.AppOptimizeEnum
import com.venus.backgroundopt.xposed.manager.process.ProcessingResult
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2023/9/26
 */
open class ProcessRecordBaseInfo(
    var uid: Int = Int.MIN_VALUE,
    var pid: Int = Int.MIN_VALUE,
    var userId: Int = 0,
): MessageFlag {
    /**
     * 系统设置的oom_score_adj
     */
    var oomAdjScore: Int = Int.MIN_VALUE

    // 当前oom
    var curAdj: Int = Int.MIN_VALUE

    // 资源占用
    var rssInBytes: Long = Long.MIN_VALUE

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    @Volatile
    var fixedOomAdjScore = Int.MIN_VALUE

    @Volatile
    var originalMaxAdj = ProcessAdjConstants.MAX_ADJ //ProcessList.UNKNOWN_ADJ

    // 是否是app主进程
    @Volatile
    var mainProcess = false

    lateinit var packageName: String
    lateinit var processName: String

    val lastProcessingResultMap =
        ConcurrentHashMap<AppOptimizeEnum, ProcessingResult>(2)

    // 是否是webview进程
    var webviewProcess = false
    var webviewProcessProbable = webviewProcess   /* 更宽泛的匹配条件 */

    fun initLastProcessingResultIfAbsent(
        appOptimizeEnum: AppOptimizeEnum,
        processingResultSupplier: () -> ProcessingResult,
    ): ProcessingResult {
        return lastProcessingResultMap.computeIfAbsent(appOptimizeEnum) { _ ->
            processingResultSupplier()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessRecordBaseInfo) return false

        if (pid != other.pid) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pid
        result = 31 * result + userId
        return result
    }


    companion object {
        /**
         * 获取用于ui显示的资源占用大小
         * @param rssInBytes Long 以Bytes为单位的资源占用大小
         * @return String ui最终显示的资源占用大小
         */
        @JvmStatic
        fun getRssUiText(rssInBytes: Long): String {
            val rssInKBytes = rssInBytes / 1024
            if (rssInKBytes < 1024) {
                if (rssInKBytes < 0) {
                    return "UNKNOWN"
                }
                return "${rssInKBytes} KB"
            }

            val rssInMBytes = rssInKBytes / 1024
            if (rssInMBytes < 1024) {
                return "${rssInMBytes} MB"
            }

            val rssInGBytes = rssInMBytes / 1024
            return "${rssInGBytes} GB"
        }
    }
}