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
                    
 package com.venus.backgroundopt.hook.handle.android.entity

import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.findClass
import com.venus.backgroundopt.utils.log.logError
import com.venus.backgroundopt.utils.log.logInfo
import com.venus.backgroundopt.utils.newInstanceXp

/**
 * @author XingC
 * @date 2023/11/6
 */
class MemInfoReader private constructor(private val memInfoReader: Any) {
    companion object {
        var clazz: Class<*>? = null

        @JvmStatic
        fun getInstance(classLoader: ClassLoader): MemInfoReader? {
            clazz ?: run {
                clazz = "com.android.internal.util.MemInfoReader".findClass(classLoader)
            }
            return try {
                clazz?.newInstanceXp()
                    ?.let { instance ->
                        MemInfoReader(instance).apply {
                            readMemInfo()
                            logInfo(logStr = "内存读取器加载成功, 总内存: ${getTotalSize() / (1024 * 1024)}MB")
                        }
                    }
            } catch (t: Throwable) {
                logError(logStr = "内存读取器加载失败", t = t)
                null
            }
        }
    }

    fun readMemInfo() {
        memInfoReader.callMethod("readMemInfo")
    }

    /**
     * Total amount of RAM available to the kernel.
     */
    fun getTotalSize(): Long {
        return memInfoReader.callMethod("getTotalSize") as Long
    }

    /**
     * Amount of RAM that is not being used for anything.
     */
    fun getFreeSize(): Long {
        return memInfoReader.callMethod("getFreeSize") as Long
    }

    /**
     * Amount of RAM that the kernel is being used for caches, not counting caches
     * that are mapped in to processes.
     */
    fun getCachedSize(): Long {
        return memInfoReader.callMethod("getCachedSize") as Long
    }

    /**
     * Amount of RAM that is in use by the kernel for actual allocations.
     */
    fun getKernelUsedSize(): Long {
        return memInfoReader.callMethod("getKernelUsedSize") as Long
    }

    /**
     * Total amount of RAM available to the kernel.
     */
    fun getTotalSizeKb(): Long {
        return memInfoReader.callMethod("getTotalSizeKb") as Long
    }

    /**
     * Amount of RAM that is not being used for anything.
     */
    fun getFreeSizeKb(): Long {
        return memInfoReader.callMethod("getFreeSizeKb") as Long
    }

    /**
     * Amount of RAM that the kernel is being used for caches, not counting caches
     * that are mapped in to processes.
     */
    fun getCachedSizeKb(): Long {
        return memInfoReader.callMethod("getCachedSizeKb") as Long
    }

    /**
     * Amount of RAM that is in use by the kernel for actual allocations.
     */
    fun getKernelUsedSizeKb(): Long {
        return memInfoReader.callMethod("getKernelUsedSizeKb") as Long
    }

    fun getSwapTotalSizeKb(): Long {
        return memInfoReader.callMethod("getSwapTotalSizeKb") as Long
    }

    fun getSwapFreeSizeKb(): Long {
        return memInfoReader.callMethod("getSwapFreeSizeKb") as Long
    }

    fun getZramTotalSizeKb(): Long {
        return memInfoReader.callMethod("getZramTotalSizeKb") as Long
    }

    fun getRawInfo(): LongArray {
        return memInfoReader.callMethod("getRawInfo") as LongArray
    }
}