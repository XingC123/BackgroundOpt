package com.venus.backgroundopt.hook.handle.android

import android.os.IBinder
import android.os.WorkSource
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.afterHook
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2024/3/9
 */
class PowerManagerServiceHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    val wakeLockMap = ConcurrentHashMap<IBinder, ProcessRecordKt>(8)

    override fun hook() {
        ClassConstants.PowerManagerService.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.acquireWakeLockInternal,
            paramTypes = arrayOf(
                IBinder::class.java,    /* lock */
                Int::class.java,        /* displayId */
                Int::class.java,        /* flags */
                String::class.java,     /* tag */
                String::class.java,     /* packageName */
                WorkSource::class.java, /* ws */
                String::class.java,     /* historyTag */
                Int::class.java,        /* uid */
                Int::class.java,        /* pid */
                ClassConstants.IWakeLockCallback,  /* callback */
            ),
        ) { param ->
            val lock = param.args[0] as IBinder
            val flags = param.args[2] as Int
            val pid = param.args[8] as Int
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@afterHook
            wakeLockMap.computeIfAbsent(lock) { processRecord.also { it.incrementWakeLockCount() } }

            if (BuildConfig.DEBUG) {
                logger.debug("pid: ${pid}, packageName: [${processRecord.packageName}], processName: [${processRecord.processName}] 正在请求唤醒锁, tag: ${param.args[3]}, flags: ${flags}")
            }
        }

        ClassConstants.PowerManagerService.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.releaseWakeLockInternal,
            hookAllMethod = true
        ) { param ->
            val lock = param.args[0] as IBinder
            wakeLockMap.remove(lock)?.let { processRecord ->
                processRecord.decrementWakeLockCount()

                if (BuildConfig.DEBUG) {
                    logger.debug("pid: ${processRecord.pid}, packageName: [${processRecord.packageName}], processName: [${processRecord.processName}] 释放唤醒锁")
                }
            }
        }
    }
}