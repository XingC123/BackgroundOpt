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

package com.venus.backgroundopt.xposed.point.android

import android.os.SystemClock
import com.venus.backgroundopt.common.environment.Features
import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalMethodReplacement
import com.venus.backgroundopt.xposed.annotation.OriginalObjectField
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.android.app.ActivityManagerInternal
import com.venus.backgroundopt.xposed.entity.android.android.app.ApplicationExitInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ActivityManagerConstants
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.AppProfiler
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.AppProfiler.AppProfilerHelper
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.OomAdjuster
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessCachedOptimizerRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessList
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessServiceRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessStateRecord
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.afterConstructorHook
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getIntFieldValue
import com.venus.backgroundopt.xposed.util.getObjectFieldValue
import com.venus.backgroundopt.xposed.util.replaceHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam


/**
 * @author XingC
 * @date 2024/2/1
 */
class OomAdjusterHookNew(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    @OriginalObjectField
    private lateinit var oomAdjuster: Any

    @OriginalObjectField
    private lateinit var mProcessList: Any

    @OriginalObjectField
    private lateinit var mService: Any

    @OriginalObjectField
    private lateinit var mConstants: Any

    // List<ProcessRecord>
    @OriginalObjectField
    private lateinit var lruList: List<*>
    private lateinit var mAppProfiler: AppProfiler

    private fun applyOomAdjLSP(
        processRecord: Any,
        now: Long,
        nowElapsed: Long,
        oomAdjReason: Any?,
    ) {
        applyOomAdjLSPMethod(processRecord, now, nowElapsed, oomAdjReason)
    }

    /**
     * applyOomAdjLSP方法是否含有oomAdjReason字段
     *
     * 通常情况下, 按照aosp源码做即可。但存在例外情况。
     *
     * 在红米note5 pro(whyred)的Nusantara v5.3 official a13中, 此方法的入参并不同于aosp, 扩展一下,
     * 可能某些rom也会对此修改。因此为了模块的通用性, 改用在第一次使用时动态生成。
     */
    @Volatile
    private var hasOomAdjReasonField: Boolean = OsUtils.isUOrHigher

    private val applyOomAdjLSPMethodName = if (OsUtils.isSOrHigher) {
        MethodConstants.applyOomAdjLSP
    } else {
        MethodConstants.applyOomAdjLocked
    }
    private val applyOomAdjLSPMethod by lazy {
        if (hasOomAdjReasonField) {
            { processRecord: Any, now: Long, nowElapsed: Long, oomAdjReason: Any? ->
                oomAdjuster.callMethod(
                    methodName = applyOomAdjLSPMethodName,
                    processRecord,
                    true,
                    now,
                    nowElapsed,
                    oomAdjReason
                )
            }
        } else {
            { processRecord: Any, now: Long, nowElapsed: Long, _: Any? ->
                oomAdjuster.callMethod(
                    methodName = applyOomAdjLSPMethodName,
                    processRecord,
                    true,
                    now,
                    nowElapsed
                )
            }
        }
    }

    private fun getOomAdjReason(param: MethodHookParam): Any? = getOomAdjReasonMethod(param.args)

    @Volatile
    private var getOomAdjReasonMethod = { paramArgs: Array<Any?> ->
        hasOomAdjReasonField = paramArgs.size > 4
        val finalMethod = if (hasOomAdjReasonField) {
            { args: Array<Any?> ->
                args[4]
            }
        } else {
            { _: Array<Any?> ->
                null
            }
        }
        resetOomAdjReasonMethod(finalMethod)
        finalMethod(paramArgs)
    }

    private fun resetOomAdjReasonMethod(oomAdjReasonMethod: (Array<Any?>) -> Any?) {
        getOomAdjReasonMethod = oomAdjReasonMethod
    }

    private val isPendingFinishAttachMethod = if (OsUtils.isUOrHigher) {
        { processRecord: Any ->
            ProcessRecord.isPendingFinishAttach(processRecord)
        }
    } else {
        { _: Any ->
            false
        }
    }

    override fun hook() {
        // 拿到一些必要的变量
        ClassConstants.OomAdjuster.afterConstructorHook(
            classLoader = classLoader,
            hookAllMethod = true
        ) { param ->
            oomAdjuster = param.thisObject
            mProcessList = oomAdjuster.getObjectFieldValue(
                fieldName = FieldConstants.mProcessList
            )!!
            mConstants = oomAdjuster.getObjectFieldValue(
                fieldName = FieldConstants.mConstants
            )!!
            lruList = if (OsUtils.isSOrHigher) {
                mProcessList.callMethod(
                    methodName = MethodConstants.getLruProcessesLOSP
                ) as List<*>
            } else {
                mProcessList.getObjectFieldValue(
                    fieldName = FieldConstants.mLruProcesses
                ) as List<*>
            }
            mService = oomAdjuster.getObjectFieldValue(
                fieldName = FieldConstants.mService
            )!!
            if (OsUtils.isSOrHigher) {
                mAppProfiler = AppProfilerHelper.instanceCreator(
                    mService.getObjectFieldValue(
                        fieldName = FieldConstants.mAppProfiler
                    )!!
                )
            }
        }

        /**
         * @see [OomAdjusterHook.handleUpdateAndTrimProcessLSP]
         */
        /*ClassConstants.OomAdjuster.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateAndTrimProcessLSP,
            hookAllMethod = true,
        ) { it.args[2] = 0 }*/
        ClassConstants.OomAdjuster.replaceHook(
            classLoader = classLoader,
            methodName = MethodConstants.updateAndTrimProcessLSP,
            hookAllMethod = true,
        ) {
            updateAndTrimProcessLSP(it)
        }

        ClassConstants.OomAdjuster.replaceHook(
            enable = OsUtils.androidVersionCode in OsUtils.S..<OsUtils.U && Features.USE_TIERED_CACHED_ADJ == true,
            classLoader = classLoader,
            methodName = MethodConstants.assignCachedAdjIfNecessary,
            hookAllMethod = true,
        ) {
            assignCachedAdjIfNecessary(it)
        }

        /**
         * @see [OomAdjusterHook.handleShouldKillExcessiveProcesses]
         */
        ClassConstants.OomAdjuster.replaceHook(
            classLoader = classLoader,
            methodName = MethodConstants.shouldKillExcessiveProcesses,
            hookAllMethod = true
        ) { false }
    }

    /* *************************************************************************
     *                                                                         *
     * updateAndTrimProcessLSP                                                 *
     *                                                                         *
     **************************************************************************/
    private val processedOomAdjReason = arrayOf(
        ActivityManagerInternal.OOM_ADJ_REASON_ACTIVITY,
        ActivityManagerInternal.OOM_ADJ_REASON_FINISH_RECEIVER,
        ActivityManagerInternal.OOM_ADJ_REASON_UNBIND_SERVICE,
        ActivityManagerInternal.OOM_ADJ_REASON_REMOVE_PROVIDER,
        ActivityManagerInternal.OOM_ADJ_REASON_UI_VISIBILITY,
        ActivityManagerInternal.OOM_ADJ_REASON_PROCESS_END,
        ActivityManagerInternal.OOM_ADJ_REASON_SHORT_FGS_TIMEOUT,
        ActivityManagerInternal.OOM_ADJ_REASON_REMOVE_TASK,
        ActivityManagerInternal.OOM_ADJ_REASON_UID_IDLE,
        ActivityManagerInternal.OOM_ADJ_REASON_STOP_SERVICE,
        ActivityManagerInternal.OOM_ADJ_REASON_COMPONENT_DISABLED,
    )

    /**
     * 对[ClassConstants.OomAdjuster].[MethodConstants.updateAndTrimProcessLSP]的重写
     *
     * 省去了一些逻辑
     * @param param MethodHookParam
     * @return Boolean
     */
    @OriginalMethodReplacement(
        classPath = ClassConstants.OomAdjuster,
        methodName = MethodConstants.updateAndTrimProcessLSP
    )
    private fun updateAndTrimProcessLSP(param: MethodHookParam): Boolean {
        val now = param.args[0] as Long
        val nowElapsed = param.args[1] as Long
        val oomAdjReason = getOomAdjReason(param)

        val numLru = lruList.size

        for (i in numLru - 1 downTo 0) {
            // ProcessRecord
            val app: Any = lruList[i]!!
            // ProcessStateRecord
            val state: Any = ProcessRecord.getProcessStateRecord(app)
            if (!ProcessRecord.isKilledByAm(app) && ProcessRecord.getThread(app) != null) {
                // We don't need to apply the update for the process which didn't get computed
                if (ProcessStateRecord.getCompletedAdjSeq(state) == oomAdjuster.getIntFieldValue(
                        fieldName = FieldConstants.mAdjSeq
                    )
                ) {
                    applyOomAdjLSP(
                        processRecord = app,
                        now = now,
                        nowElapsed = nowElapsed,
                        oomAdjReason = oomAdjReason
                    )
                }
                if (isPendingFinishAttachMethod(app)) {
                    // Avoid trimming processes that are still initializing. If they aren't
                    // hosting any components yet because they may be unfairly killed.
                    // We however apply the oom scores set at #setAttachingProcessStatesLSP.
                    continue
                }
                // ProcessServiceRecord
                val psr: Any = ProcessRecord.getProcessServiceRecord(app)
                val numberOfRunningServices = ProcessServiceRecord.numberOfRunningServices(psr)

                if (ProcessRecord.isIsolated(app)
                    && numberOfRunningServices <= 0
                    && ProcessRecord.getIsolatedEntryPoint(app) == null
                ) {
                    // If this is an isolated process, there are no services
                    // running in it, and it's not a special process with a
                    // custom entry point, then the process is no longer
                    // needed.  We agressively kill these because we can by
                    // definition not re-use the same process again, and it is
                    // good to avoid having whatever code was running in them
                    // left sitting around after no longer needed.
                    ProcessRecord.killLocked(
                        instance = app,
                        reason = "isolated not needed",
                        reasonCode = ApplicationExitInfo.REASON_OTHER,
                        subReason = ApplicationExitInfo.SUBREASON_ISOLATED_NOT_NEEDED,
                        noisy = true
                    )
                } else if (OsUtils.isUOrHigher && ProcessRecord.isSdkSandbox(app)
                    && numberOfRunningServices <= 0
                    && ProcessRecord.getActiveInstrumentation(app) == null
                ) {
                    // If this is an SDK sandbox process and there are no services running it, we
                    // aggressively kill the sandbox as we usually don't want to re-use the same
                    // sandbox again.
                    ProcessRecord.killLocked(
                        instance = app,
                        reason = "sandbox not needed",
                        reasonCode = ApplicationExitInfo.REASON_OTHER,
                        subReason = ApplicationExitInfo.SUBREASON_SDK_SANDBOX_NOT_NEEDED,
                        noisy = true
                    )
                } else {
                    // Keeping this process, update its uid.
                    OomAdjuster.updateAppUidRecLSP(oomAdjuster, app)
                }
            }
        }

        return updateLowMemStateLSP(now)
    }

    private fun updateLowMemStateLSP(now: Long): Boolean = updateLowMemStateLSPMethod(now)

    private val updateLowMemStateLSPMethod: (Long) -> Boolean = if (OsUtils.isSOrHigher) {
        { now ->
            mAppProfiler.updateLowMemStateLSP(
                numCached = 0,
                numEmpty = 0,
                numTrimming = 0,
                now = now,
            )
        }
    } else {
        { now ->
            mService.callMethod<Boolean>(
                methodName = MethodConstants.updateLowMemStateLocked,
                0,
                0,
                0
            )
        }
    }

    /**
     * 对[ClassConstants.OomAdjuster].[MethodConstants.assignCachedAdjIfNecessary]的重写
     *
     * 为a14以下版本开启分级缓存
     *
     * @param param MethodHookParam
     */
    @OriginalMethodReplacement(
        classPath = ClassConstants.OomAdjuster,
        methodName = MethodConstants.assignCachedAdjIfNecessary
    )
    private fun assignCachedAdjIfNecessary(param: MethodHookParam) {
        val lruList = param.args[0] as List<*>

        val numLru: Int = lruList.size
        val now = SystemClock.uptimeMillis()
        for (i in numLru - 1 downTo 0) {
            // ProcessRecord
            val app: Any = lruList[i]!!
            // ProcessStateRecord
            val state: Any = ProcessRecord.getProcessStateRecord(app)
            // ProcessCachedOptimizerRecord
            val opt: Any? = ProcessRecord.getProcessCachedOptimizerRecord(app)
            if (!ProcessRecord.isKilledByAm(app) && ProcessRecord.getThread(app) != null
                && ProcessStateRecord.getCurAdj(state) >= ProcessList.UNKNOWN_ADJ
            ) {
                // ProcessServiceRecord
                val psr: Any = ProcessRecord.getProcessServiceRecord(app)
                var targetAdj = ProcessList.CACHED_APP_MIN_ADJ
                targetAdj += if (opt != null && ProcessCachedOptimizerRecord.isFreezeExempt(opt)) {
                    // BIND_WAIVE_PRIORITY and the like get oom_adj 900
                    0
                } else if (ProcessStateRecord.getSetAdj(state) >= ProcessList.CACHED_APP_MIN_ADJ
                    && (ProcessStateRecord.getLastStateTime(state) + ActivityManagerConstants.TIERED_CACHED_ADJ_DECAY_TIME) < now
                ) {
                    // Older cached apps get 950
                    50
                } else {
                    // Newer cached apps get 910
                    10
                }
                ProcessStateRecord.setCurRawAdj(state, targetAdj)
                ProcessStateRecord.setCurAdj(
                    state,
                    ProcessServiceRecord.modifyRawOomAdj(psr, targetAdj)
                )
            }
        }
    }
}