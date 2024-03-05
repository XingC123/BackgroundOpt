package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.findClassIfExists
import com.venus.backgroundopt.utils.getLongFieldValue
import com.venus.backgroundopt.utils.replaceHook
import com.venus.backgroundopt.utils.runCatchThrowable
import com.venus.backgroundopt.utils.setStaticObjectFieldValue

/**
 * @author XingC
 * @date 2024/2/9
 */
class CachedAppOptimizerHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        // 模块接管了压缩行为
        ClassConstants.CachedAppOptimizer.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.onOomAdjustChanged,
            hookAllMethod = true
        ) { it.result = null }

        // 关闭系统的内存压缩
        runCatchThrowable(catchBlock = { logger.warn("设置系统的默认压缩行为失败", it) }) {
            ClassConstants.CachedAppOptimizer
                .findClassIfExists(classLoader)
                ?.setStaticObjectFieldValue(
                    fieldName = FieldConstants.DEFAULT_USE_COMPACTION,
                    value = false
                )
            logger.info("[禁用]框架层内存压缩")
        }

        ClassConstants.CachedAppOptimizer.replaceHook(
            classLoader = classLoader,
            methodName = MethodConstants.useCompaction,
        ) { false }

        // hook系统freeze进程的延迟时间
        ClassConstants.CachedAppOptimizer.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateFreezerDebounceTimeout,
        ) {
            val cachedAppOptimizerInstance =
                runningInfo.activityManagerService.oomAdjuster.cachedAppOptimizer.getInstance()
            val fieldValue =
                cachedAppOptimizerInstance.getLongFieldValue(FieldConstants.mFreezerDebounceTimeout)
            runningInfo.activityManagerService.oomAdjuster.cachedAppOptimizer.mFreezerDebounceTimeout =
                fieldValue

            logger.info("系统freeze进程的延迟时间: ${fieldValue}")
        }
    }
}