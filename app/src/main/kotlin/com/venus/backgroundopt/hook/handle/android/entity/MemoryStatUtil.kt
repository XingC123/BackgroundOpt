package com.venus.backgroundopt.hook.handle.android.entity

import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
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