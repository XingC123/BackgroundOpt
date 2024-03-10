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

import android.app.usage.UsageEvents
import android.content.ComponentName
import android.content.Intent
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.base.generateMatchedMethodHookPoint
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.getStaticIntFieldValue
import com.venus.backgroundopt.utils.message.registeredMessageHandler
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/19
 */
class ActivityManagerServiceHookKt(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    companion object {
        const val ACTIVITY_RESUMED = UsageEvents.Event.ACTIVITY_RESUMED
        const val ACTIVITY_PAUSED = UsageEvents.Event.ACTIVITY_PAUSED
        const val ACTIVITY_STOPPED = UsageEvents.Event.ACTIVITY_STOPPED

        @JvmField
        val ACTIVITY_DESTROYED =
            UsageEvents.Event::class.java.getStaticIntFieldValue(FieldConstants.ACTIVITY_DESTROYED)
    }

    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            generateMatchedMethodHookPoint(
                true,
                ClassConstants.ActivityManagerService,
                MethodConstants.updateActivityUsageStats,
                arrayOf(
                    beforeHookAction { handleUpdateActivityUsageStats(it) }
                )
            ),
//            HookPoint(
//                ClassConstants.ActivityManagerService,
//                MethodConstants.forceStopPackage,
//                arrayOf(
//                    afterHookAction {
//                        handleForceStopPackage(it)
//                    }
//                ),
//                String::class.java, /* packageName */
//                Int::class.java /* userId */
//            ),
//            HookPoint(
//                ClassConstants.ActivityManagerService,
//                MethodConstants.cleanUpApplicationRecordLocked,
//                arrayOf(
//                    afterHookAction {
//                        handleCleanUpApplicationRecordLocked(it)
//                    }
//                ),
//                ClassConstants.ProcessRecord,
//                Int::class.java,    /* pid */
//                Boolean::class.java,    /* restarting */
//                Boolean::class.java,    /* allowRestart */
//                Int::class.java,    /* index */
//                Boolean::class.java,    /* replacingPid */
//                Boolean::class.java /* fromBinderDied */
//            ),
            generateMatchedMethodHookPoint(
                true,
                ClassConstants.ActivityManagerService,
                MethodConstants.removePidLocked,
                arrayOf(
                    beforeHookAction { handleRemovePidLocked(it) }
                )
            ),
            HookPoint(
                ClassConstants.ActivityManagerService,
                MethodConstants.killProcessesBelowAdj,
                arrayOf(
                    beforeHookAction {
                        handleKillProcessesBelowAdj(it)
                    }
                ),
                Int::class.java,    /* belowAdj */
                String::class.java  /* reason */
            ),
            HookPoint(
                ClassConstants.ActivityManagerService,
                MethodConstants.startService,
                arrayOf(
                    beforeHookAction { handleStartService(it) }
                ),
                ClassConstants.IApplicationThread,      // caller
                Intent::class.java,                     // service
                String::class.java,                     // resolvedType
                Boolean::class.java,                    // requireForeground
                String::class.java,                     // callingPackage
                String::class.java,                     // callingFeatureId
                Int::class.java                         // userId
            ),
            /*generateMatchedMethodHookPoint(
                true,
                ClassConstants.ActivityManagerService,
                MethodConstants.performIdleMaintenance,
                arrayOf(
                    doNothingHookAction()
                )
            ),*/
        )
    }

    /* *************************************************************************
     *                                                                         *
     * 前后台切换                                                                *
     *                                                                         *
     **************************************************************************/
    private val handleEvents = arrayOf(
        ACTIVITY_RESUMED,
        ACTIVITY_STOPPED,
    )

    fun handleUpdateActivityUsageStats(param: MethodHookParam) {
        // 获取方法参数
        val args = param.args

        // 获取切换事件
        val event = args[2] as Int
        if (event !in handleEvents) {
            return
        }

        // 本次事件包名
        val componentName = (args[0] as? ComponentName) ?: return
        // 本次事件用户
        val userId = args[1] as Int

        runningInfo.handleActivityEventChange(event, userId, componentName)
    }

    /* *************************************************************************
     *                                                                         *
     * 停止app后台                                                               *
     *                                                                         *
     **************************************************************************/
    private fun handleForceStopPackage(param: MethodHookParam) {
        val packageName = param.args[0] as String
        val userId = param.args[1] as Int

        val appInfo = runningInfo.getRunningAppInfo(userId, packageName) ?: return
        runningInfo.removeRunningApp(appInfo)
    }

    @Deprecated("有时候不执行。比如在王者荣耀里面两次返回从游戏内退出")
    private fun handleCleanUpApplicationRecordLocked(param: MethodHookParam) {
        val pid = param.args[1] as Int

        runningInfo.removeProcess(pid)
    }

    fun handleRemovePidLocked(param: MethodHookParam) {
        val pid = param.args[0] as Int

        runningInfo.removeProcess(pid)
    }

    private fun handleKillProcessesBelowAdj(param: MethodHookParam) {
        // 拔高adj分数
        param.args[0] = ProcessRecordKt.SUB_PROC_ADJ
    }

    /**
     * ui消息监听
     */
    private fun handleStartService(param: MethodHookParam) {
        val dataIntent = param.args[1] as Intent
        if (dataIntent.`package` != BuildConfig.APPLICATION_ID) {
            return
        }

        val key = dataIntent.type ?: return       // 此次请求的key
        val value = dataIntent.action   // 请求的值

        registeredMessageHandler[key]?.handle(runningInfo, param, value)
    }
}