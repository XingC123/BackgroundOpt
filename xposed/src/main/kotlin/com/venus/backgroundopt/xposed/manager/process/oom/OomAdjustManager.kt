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

package com.venus.backgroundopt.xposed.manager.process.oom

import com.venus.backgroundopt.common.entity.preference.OomWorkModePref
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.concurrent.lock.lock
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.self.AppInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.manager.process.oom.OomAdjHandler.Companion.ADJ_TASK_PRIORITY_NORMAL
import com.venus.backgroundopt.xposed.point.android.function.ActivitySwitchHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2024/7/17
 */
class OomAdjustManager(
    val runningInfo: RunningInfo,
) {
    private val useSimpleLmk: Boolean get() = HookCommonProperties.useSimpleLmk

    private var oomAdjHandler: OomAdjHandler = initOomAdjHandler()
    private val adjHandleActionPool = ExecutorUtils.newFixedThreadPool(
        coreSize = 3,
        factoryName = "adjHandleActionPool"
    )

    private fun initOomAdjHandler(): OomAdjHandler {
        return when (HookCommonProperties.oomWorkModePref.oomMode) {
            OomWorkModePref.MODE_STRICT -> StrictModeOomAdjHandler()
            OomWorkModePref.MODE_BALANCE -> {
                if (!useSimpleLmk) {
                    BalanceModeOomAdjHandler()
                } else {
                    SimpleLmkOomAdjHandler()
                }
            }

            OomWorkModePref.MODE_BALANCE_PLUS -> {
                if (!useSimpleLmk) {
                    BalancePlusModeOomAdjHandler()
                } else {
                    SimpleLmkOomAdjHandler()
                }
            }

            OomWorkModePref.MODE_NEGATIVE -> NegativeModeOomAdjHandler()
            else -> {
                StrictSecondaryModeOomAdjHandler()
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * ADJ的设置                                                                *
     *                                                                         *
     **************************************************************************/
    private fun addAdjHandleAction(block: () -> Unit) {
        adjHandleActionPool.execute(block)
    }

    @JvmOverloads
    fun addTask(
        processRecord: ProcessRecord,
        priority: Int = ADJ_TASK_PRIORITY_NORMAL,
        block: () -> Unit,
    ) {
        oomAdjHandler.addTask(processRecord, priority, block)
    }

    fun computeHighPrioritySubprocessAdj(adj: Int): Int {
        return oomAdjHandler.computeHighPrioritySubprocessAdj(adj)
    }

    fun handleSetOomAdj(param: MethodHookParam) {
        val pid = param.args[0] as Int
        // 获取当前进程对象
        val process = runningInfo.getRunningProcess(pid) ?: return
        val appInfo = process.appInfo
        val appGroupEnum = appInfo.appGroupEnum

        // app已死亡
        if (appGroupEnum == AppGroupEnum.DEAD) {
            return
        }

        // 本次要设置的adj
        val adj = param.args[2] as Int

        val globalOomScorePolicy = HookCommonProperties.globalOomScorePolicy.value
        if (!globalOomScorePolicy.enabled) {
            // 若app未进入后台, 则不进行设置
            /*if (appInfo.appGroupEnum !in processedAppGroup) {
                return
            }*/
            when (appGroupEnum) {
                AppGroupEnum.NONE, AppGroupEnum.ACTIVE, AppGroupEnum.IDLE -> {
                    // 将会被处理
                }

                else -> {
                    return
                }
            }
        }

        param.result = null
        addAdjHandleAction {
            appInfo.lock {
                handleSetOomAdjLocked(
                    process = process,
                    adj = adj,
                    appInfo = appInfo,
                    appGroupEnum = appGroupEnum
                )
            }
        }
    }

    @JvmOverloads
    fun handleSetOomAdjLocked(
        process: ProcessRecord,
        adj: Int,
        priority: Int = ADJ_TASK_PRIORITY_NORMAL,
        appInfo: AppInfo = process.appInfo,
        appGroupEnum: AppGroupEnum = appInfo.appGroupEnum,
    ) {
        val mainProcess = process.mainProcess
        val adjLastSet = process.oomAdjScore
        var oomAdjustLevel = OomAdjustLevel.NONE

        // 计算并应用adj
        val adjWillSet = oomAdjHandler.getAdjWillSet(process, adj)
        oomAdjHandler.computeAdjAndApply(process, adj, priority)

        // 记录本次系统计算的分数
        process.oomAdjScore = adj

        // ProcessListHookKt.handleHandleProcessStartedLocked 执行后, 生成进程所属的appInfo
        // 但并没有将其设置内存分组。若在进程创建时就设置appInfo的内存分组, 则在某些场景下会产生额外消耗。
        // 例如, 在打开新app时, 会首先创建进程, 紧接着显示界面。分别会执行: ①ProcessListHookKt.handleHandleProcessStartedLocked
        // ② ActivityManagerServiceHookKt.handleUpdateActivityUsageStats。
        // 此时, 若在①中设置了分组, 在②中会再次设置。即: 新打开app需要连续两次appInfo的内存分组迁移, 这是不必要的。
        // 我们的目标是保活以及额外处理, 那么只需在①中将其放入running.runningApps, 在设置oom时就可以被管理。
        // 此时那些没有打开过页面的app就可以被设置内存分组, 相应的进行内存优化处理。
        if (appGroupEnum == AppGroupEnum.NONE && mainProcess) {
            runningInfo.handleActivityEventChangeLocked(
                ActivitySwitchHook.ACTIVITY_STOPPED,
                null,
                appInfo
            )
        }

        // 内存压缩
        runningInfo.processManager.compactProcess(
            process,
            adjLastSet,
            adjWillSet,
            oomAdjustLevel
        )
    }

    /* *************************************************************************
     *                                                                         *
     * 全局属性监听                                                               *
     *                                                                         *
     **************************************************************************/
    object AdjHandleActionTypeListenerConstants {
        const val GLOBAL_ADJ = "ADJ_HANDLE_ACTION_TYPE_GLOBAL_ADJ_LISTENER_KEY"
        const val WEBVIEW_PROCESS_PROTECT = "ADJ_HANDLE_ACTION_TYPE_GLOBAL_ADJ_LISTENER_KEY"
    }

    init {
        addPropertyChangeListener()
    }

    private fun addPropertyChangeListener() {
        HookCommonProperties.enableWebviewProcessProtect.addListener(
            AdjHandleActionTypeListenerConstants.WEBVIEW_PROCESS_PROTECT
        ) { _, _ ->
            printAdjHandleActionTypeLog("webview进程保护切换")
            ProcessRecord.resetAdjHandleType()
        }
    }

    companion object {
        fun printAdjHandleActionTypeLog(tag: String) {
            ILogger.getLoggerStatic(OomAdjustManager::class.java)
                .info("重新加载adj处理策略: [${tag}]")
        }
    }
}

/**
 * 进程OOM调整等级
 */
class OomAdjustLevel {
    companion object {
        const val NONE = 0
        const val FIRST = 1
        const val SECOND = 2
    }
}