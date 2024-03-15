package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.afterConstructorHook
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.setIntFieldValue

/**
 * @author XingC
 * @date 2024/3/12
 */
class ActivityManagerConstantsHookNew(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.ActivityManagerConstants.afterConstructorHook(
            classLoader = classLoader,
            hookAllMethod = true,
        ) { param ->
            val activityManagerConstants = param.thisObject

            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.mOverrideMaxCachedProcesses,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.mCustomizedMaxCachedProcesses,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.MAX_PHANTOM_PROCESSES,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.MAX_CACHED_PROCESSES,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.CUR_MAX_CACHED_PROCESSES,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.CUR_MAX_EMPTY_PROCESSES,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.CUR_TRIM_CACHED_PROCESSES,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.CUR_TRIM_EMPTY_PROCESSES,
                value = Int.MAX_VALUE
            )
        }

        ClassConstants.ActivityManagerConstants.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.setOverrideMaxCachedProcesses,
            paramTypes = arrayOf(Int::class.java),
        ) { param ->
            param.args[0] = Int.MAX_VALUE
        }

        ClassConstants.ActivityManagerConstants.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateMaxCachedProcesses,
        ) { param ->
            param.result = null
        }

        ClassConstants.ActivityManagerConstants.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateMaxPhantomProcesses,
        ) { param ->
            param.result = null
        }
    }
}