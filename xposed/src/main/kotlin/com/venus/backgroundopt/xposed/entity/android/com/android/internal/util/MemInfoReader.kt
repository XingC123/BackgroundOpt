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

package com.venus.backgroundopt.xposed.entity.android.com.android.internal.util

import com.venus.backgroundopt.common.util.log.logError
import com.venus.backgroundopt.common.util.log.logInfo
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.findClass
import com.venus.backgroundopt.xposed.util.newInstanceXp

/**
 * @author XingC
 * @date 2023/11/6
 */
class MemInfoReader private constructor(
    override val originalInstance: Any,
) : IEntityWrapper {
    companion object {
        val clazz: Class<*>? by lazy {
            val classLoader = RunningInfo.getInstance().classLoader
            ClassConstants.MemInfoReader.findClass(classLoader)
        }

        @JvmStatic
        fun getInstance(): MemInfoReader? {
            return try {
                clazz?.newInstanceXp()?.let { instance ->
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
        originalInstance.callMethod("readMemInfo")
    }

    /**
     * Total amount of RAM available to the kernel.
     */
    fun getTotalSize(): Long {
        return originalInstance.callMethod("getTotalSize") as Long
    }

    /**
     * Amount of RAM that is not being used for anything.
     */
    fun getFreeSize(): Long {
        return originalInstance.callMethod("getFreeSize") as Long
    }

    /**
     * Amount of RAM that the kernel is being used for caches, not counting caches
     * that are mapped in to processes.
     */
    fun getCachedSize(): Long {
        return originalInstance.callMethod("getCachedSize") as Long
    }

    /**
     * Amount of RAM that is in use by the kernel for actual allocations.
     */
    fun getKernelUsedSize(): Long {
        return originalInstance.callMethod("getKernelUsedSize") as Long
    }

    /**
     * Total amount of RAM available to the kernel.
     */
    fun getTotalSizeKb(): Long {
        return originalInstance.callMethod("getTotalSizeKb") as Long
    }

    /**
     * Amount of RAM that is not being used for anything.
     */
    fun getFreeSizeKb(): Long {
        return originalInstance.callMethod("getFreeSizeKb") as Long
    }

    /**
     * Amount of RAM that the kernel is being used for caches, not counting caches
     * that are mapped in to processes.
     */
    fun getCachedSizeKb(): Long {
        return originalInstance.callMethod("getCachedSizeKb") as Long
    }

    /**
     * Amount of RAM that is in use by the kernel for actual allocations.
     */
    fun getKernelUsedSizeKb(): Long {
        return originalInstance.callMethod("getKernelUsedSizeKb") as Long
    }

    fun getSwapTotalSizeKb(): Long {
        return originalInstance.callMethod("getSwapTotalSizeKb") as Long
    }

    fun getSwapFreeSizeKb(): Long {
        return originalInstance.callMethod("getSwapFreeSizeKb") as Long
    }

    fun getZramTotalSizeKb(): Long {
        return originalInstance.callMethod("getZramTotalSizeKb") as Long
    }

    fun getRawInfo(): LongArray {
        return originalInstance.callMethod("getRawInfo") as LongArray
    }
}