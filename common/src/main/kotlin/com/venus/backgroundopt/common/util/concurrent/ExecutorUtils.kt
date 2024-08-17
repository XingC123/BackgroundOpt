package com.venus.backgroundopt.common.util.concurrent

import android.os.Process
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author XingC
 * @date 2024/7/19
 */
object ExecutorUtils {
    @JvmStatic
    private fun getFactory(
        factoryName: String? = null,
        threadFactory: ThreadFactory? = null,
    ): ThreadFactory {
        return threadFactory ?: CommonThreadFactory(factoryName)
    }

    @JvmStatic
    @JvmOverloads
    fun newFixedThreadPool(
        coreSize: Int,
        factoryName: String? = null,
        threadFactory: ThreadFactory? = null,
    ): ExecutorService {
        return Executors.newFixedThreadPool(coreSize, getFactory(factoryName, threadFactory))
    }

    @JvmStatic
    @JvmOverloads
    fun newScheduleThreadPool(
        coreSize: Int,
        factoryName: String? = null,
        threadFactory: ThreadFactory? = null,
        removeOnCancelPolicy: Boolean = false,
    ): ScheduledThreadPoolExecutor {
        return ScheduledThreadPoolExecutor(coreSize, getFactory(factoryName, threadFactory)).apply {
            this.removeOnCancelPolicy = removeOnCancelPolicy
        }
    }
}

class CommonThreadFactory(
    name: String? = null,
    val mPriority: Int = Process.THREAD_PRIORITY_BACKGROUND,
) : ThreadFactory {
    private val threadNumber = AtomicInteger(1)
    private val factoryName = name ?: getThreadFactoryName()

    override fun newThread(r: Runnable): Thread {
        return CommonThread(
            threadName = getThreadName(),
            mPriority = mPriority,
            runnable = r
        )
    }

    private fun getThreadName(): String {
        return "${factoryName}-${THREAD_NAME}-${threadNumber.getAndIncrement()}"
    }

    private class CommonThread(
        threadName: String,
        val mPriority: Int,
        val runnable: Runnable
    ): Thread(threadName) {
        override fun run() {
            Process.setThreadPriority(mPriority)
            runnable.run()
        }
    }

    companion object {
        private val threadFactoryNumber = AtomicInteger(1)
        private const val THREAD_FACTORY_NAME = "CommonThreadFactory"
        private const val THREAD_NAME = "CommonThread"

        private fun getThreadFactoryName(): String {
            return "${THREAD_FACTORY_NAME}-${threadFactoryNumber.getAndIncrement()}"
        }
    }
}