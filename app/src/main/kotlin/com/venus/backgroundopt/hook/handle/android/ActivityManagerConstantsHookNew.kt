package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerConstants
import com.venus.backgroundopt.utils.SystemUtils
import com.venus.backgroundopt.utils.afterConstructorHook
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.beforeHook
import com.venus.backgroundopt.utils.getBooleanFieldValue
import com.venus.backgroundopt.utils.getLongFieldValue
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
                fieldName = FieldConstants.CUR_MAX_CACHED_PROCESSES,
                value = Int.MAX_VALUE
            )
            activityManagerConstants.setIntFieldValue(
                fieldName = FieldConstants.CUR_MAX_EMPTY_PROCESSES,
                value = Int.MAX_VALUE / 2
            )
            /*activityManagerConstants.setIntFieldValue(
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
            )*/
        }

        ClassConstants.ActivityManagerConstants.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.setOverrideMaxCachedProcesses,
            paramTypes = arrayOf(Int::class.java),
        ) { param ->
            param.args[0] = Int.MAX_VALUE
        }

        /*ClassConstants.ActivityManagerConstants.beforeHook(
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
        }*/
        ClassConstants.ActivityManagerConstants.afterHook(
            enable = SystemUtils.isUOrHigher,
            classLoader = classLoader,
            methodName = MethodConstants.updateUseTieredCachedAdj
        ) { param ->
            val activityManagerConstants = param.thisObject
            ActivityManagerConstants.USE_TIERED_CACHED_ADJ =
                activityManagerConstants.getBooleanFieldValue(
                    fieldName = FieldConstants.USE_TIERED_CACHED_ADJ
                )
            ActivityManagerConstants.TIERED_CACHED_ADJ_DECAY_TIME =
                activityManagerConstants.getLongFieldValue(
                    fieldName = FieldConstants.TIERED_CACHED_ADJ_DECAY_TIME
                )

            if (BuildConfig.DEBUG) {
                logger.info("USE_TIERED_CACHED_ADJ: ${ActivityManagerConstants.USE_TIERED_CACHED_ADJ}")
                logger.info("TIERED_CACHED_ADJ_DECAY_TIME: ${ActivityManagerConstants.TIERED_CACHED_ADJ_DECAY_TIME}")
            }
        }
    }
}