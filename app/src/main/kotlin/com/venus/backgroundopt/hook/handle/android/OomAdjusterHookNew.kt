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

package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.annotation.AndroidMethodReplacement
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerInternal
import com.venus.backgroundopt.hook.handle.android.entity.AppProfiler
import com.venus.backgroundopt.hook.handle.android.entity.ApplicationExitInfo
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import com.venus.backgroundopt.hook.handle.android.entity.ProcessServiceRecord
import com.venus.backgroundopt.hook.handle.android.entity.ProcessStateRecord
import com.venus.backgroundopt.utils.SystemUtils
import com.venus.backgroundopt.utils.afterConstructorHook
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.replaceHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam


/**
 * @author XingC
 * @date 2024/2/1
 */
class OomAdjusterHookNew(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    private lateinit var oomAdjuster: Any
    private lateinit var mProcessList: Any

    // List<ProcessRecord>
    private lateinit var lruList: List<*>
    private lateinit var mAppProfiler: Any

    private fun applyOomAdjLSP(processRecord: Any, now: Long, nowElapsed: Long, oomAdjReason: Int) {
        applyOomAdjLSPMethod(processRecord, now, nowElapsed, oomAdjReason)
    }

    private val applyOomAdjLSPMethod = if (SystemUtils.isUOrHigher) {
        { processRecord: Any, now: Long, nowElapsed: Long, oomAdjReason: Int ->
            oomAdjuster.callMethod(
                methodName = MethodConstants.applyOomAdjLSP,
                processRecord,
                true,
                now,
                nowElapsed,
                oomAdjReason
            )
        }
    } else {
        { processRecord: Any, now: Long, nowElapsed: Long, _: Int ->
            oomAdjuster.callMethod(
                methodName = MethodConstants.applyOomAdjLSP,
                processRecord,
                true,
                now,
                nowElapsed
            )
        }
    }

    private fun getOomAdjReason(param: MethodHookParam): Int = if (SystemUtils.isUOrHigher) {
        param.args[4] as Int
    } else {
        Int.MIN_VALUE
    }

    private val isPendingFinishAttachMethod = if (SystemUtils.isUOrHigher) {
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
            lruList = mProcessList.callMethod(
                methodName = MethodConstants.getLruProcessesLOSP
            ) as List<*>
            mAppProfiler = oomAdjuster.getObjectFieldValue(
                fieldName = FieldConstants.mService
            )!!.getObjectFieldValue(
                fieldName = FieldConstants.mAppProfiler
            )!!
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
    @AndroidMethodReplacement(
        classPath = ClassConstants.OomAdjuster,
        methodName = MethodConstants.updateAndTrimProcessLSP
    )
    private fun updateAndTrimProcessLSP(param: MethodHookParam): Boolean {
        val now = param.args[0] as Long
        /*val nowElapsed = param.args[1] as Long
        val oomAdjReason = getOomAdjReason(param)*/

        val numLru = lruList.size

        for (i in numLru - 1 downTo 0) {
            // ProcessRecord
            val app: Any = lruList[i]!!
            // ProcessStateRecord
            // val state: Any = ProcessRecord.getProcessStateRecord(app)
            if (!ProcessRecord.isKilledByAm(app) && ProcessRecord.getThread(app) != null) {
                // 在红米note5 pro(whyred)的Nusantara v5.3 official a13中, 此方法的入参并不同于aosp,
                // 扩展一下, 可能某些rom也会对此修改。因此为了模块的通用性, 遂禁用对applyOomAdjLSP的调用
                // We don't need to apply the update for the process which didn't get computed
                /*if (ProcessStateRecord.getCompletedAdjSeq(state) == oomAdjuster.getIntFieldValue(
                        fieldName = FieldConstants.mAdjSeq
                    )
                ) {
                    applyOomAdjLSP(
                        processRecord = app,
                        now = now,
                        nowElapsed = nowElapsed,
                        oomAdjReason = oomAdjReason
                    )
                }*/
                if (isPendingFinishAttachMethod(app)) {
                    // Avoid trimming processes that are still initializing. If they aren't
                    // hosting any components yet because they may be unfairly killed.
                    // We however apply the oom scores set at #setAttachingProcessStatesLSP.
                    continue
                }
                // ProcessServiceRecord
                val psr: Any = ProcessRecord.getProcessServiceRecord(app)

                if (ProcessRecord.isIsolated(app)
                    && ProcessServiceRecord.numberOfRunningServices(psr) <= 0
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
                        processRecord = app,
                        reason = "isolated not needed",
                        reasonCode = ApplicationExitInfo.REASON_OTHER,
                        subReason = ApplicationExitInfo.SUBREASON_ISOLATED_NOT_NEEDED,
                        noisy = true
                    )
                } else if (SystemUtils.isUOrHigher && ProcessRecord.isSdkSandbox(app)
                    && ProcessServiceRecord.numberOfRunningServices(psr) <= 0
                    && ProcessRecord.getActiveInstrumentation(app) == null
                ) {
                    // If this is an SDK sandbox process and there are no services running it, we
                    // aggressively kill the sandbox as we usually don't want to re-use the same
                    // sandbox again.
                    ProcessRecord.killLocked(
                        processRecord = app,
                        reason = "sandbox not needed",
                        reasonCode = ApplicationExitInfo.REASON_OTHER,
                        subReason = ApplicationExitInfo.SUBREASON_SDK_SANDBOX_NOT_NEEDED,
                        noisy = true
                    )
                } else {
                    // Keeping this process, update its uid.
                    oomAdjuster.callMethod(
                        methodName = MethodConstants.updateAppUidRecLSP,
                        app
                    )
                }
            }
        }

        return AppProfiler.updateLowMemStateLSP(
            appProfiler = mAppProfiler,
            numCached = 0,
            numEmpty = 0,
            numTrimming = 0,
            now = now,
        )
    }
}