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