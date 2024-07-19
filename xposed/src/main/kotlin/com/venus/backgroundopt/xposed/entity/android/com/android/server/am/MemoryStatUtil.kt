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

import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import de.robv.android.xposed.XposedHelpers

/**
 * 封装了安卓的[ClassConstants.MemoryStatUtil]
 *
 * @author XingC
 * @date 2023/10/13
 */
class MemoryStatUtil {
    companion object {
        private val memoryStatUtilClazz: Class<*>? by lazy {
            XposedHelpers.findClass(
                ClassConstants.MemoryStatUtil,
                RunningInfo.getInstance().classLoader
            )
        }

        @JvmStatic
                /**
                 * Reads memory stat for a process.
                 *
                 * Reads from per-app memcg if available on device, else fallback to procfs.
                 * Returns null if no stats can be read.
                 */
        fun readMemoryStatFromFilesystem(uid: Int, pid: Int): MemoryStat? {
            return memoryStatUtilClazz?.let { clazz ->
                try {
                    XposedHelpers.callStaticMethod(
                        clazz,
                        MethodConstants.readMemoryStatFromFilesystem,
                        uid,
                        pid
                    )?.let {
                        MemoryStat(it)
                    }
                } catch (t: Throwable) {
                    null
                }
            }
        }
    }

    /**
     * 对应安卓的[ClassConstants.MemoryStat]
     *
     * @property memoryStat 安卓的对象实例
     */
    class MemoryStat(private val memoryStat: Any) {
        /** Number of page faults  */
        val pgfault: Long
            get() = getValue(FieldConstants.pgfault)

        /** Number of major page faults  */
        val pgmajfault: Long
            get() = getValue(FieldConstants.pgmajfault)

        /** For memcg stats, the anon rss + swap cache size. Otherwise total RSS.  */
        val rssInBytes: Long
            get() = getValue(FieldConstants.rssInBytes)

        /** Number of bytes of page cache memory. Only present for memcg stats.  */
        val cacheInBytes: Long
            get() = getValue(FieldConstants.cacheInBytes)

        /** Number of bytes of swap usage  */
        val swapInBytes: Long
            get() = getValue(FieldConstants.swapInBytes)

        private fun getValue(fieldName: String): Long {
            return try {
                XposedHelpers.getLongField(memoryStat, fieldName)
            } catch (t: Throwable) {
                Long.MIN_VALUE
            }
        }
    }
}