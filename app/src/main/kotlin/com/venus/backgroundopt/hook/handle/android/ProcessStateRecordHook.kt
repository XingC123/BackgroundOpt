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

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.hook.handle.android.entity.ProcessStateRecord
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/25
 */
class ProcessStateRecordHook(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.ProcessStateRecord,
                MethodConstants.setCurAdj,
                arrayOf(
                    beforeHookAction { handleSetCurAdj(it) }
                ),
                Int::class.javaPrimitiveType    // curAdj
            ),
            HookPoint(
                ClassConstants.ProcessStateRecord,
                MethodConstants.getCurAdj,
                arrayOf(
                    beforeHookAction { handleGetCurAdj(it) }
                )
            ),
        )
    }

    private fun handleSetCurAdj(param: MethodHookParam) {
        val processStateRecord = param.thisObject
        val processRecordKt =
            ProcessRecordKt(
                runningInfo.activityManagerService,
                ProcessStateRecord.getProcessRecord(processStateRecord)
            )
        val appInfo = runningInfo.getRunningAppInfo(processRecordKt.uid)
        // 主进程首次创建时appInfo还未初始化, 此情况无需关心
        appInfo ?: return

        val mPid = try {
            appInfo.getmPid()
        } catch (t: Throwable) {
            Int.MIN_VALUE
        }

        if (processRecordKt.pid == mPid) {
            if (appInfo.mainProcCurAdj != ProcessRecordKt.DEFAULT_MAIN_ADJ) {
                // 放行
            } else {
                param.result = null
            }
        }
    }

    private fun handleGetCurAdj(param: MethodHookParam) {
        val processStateRecord = param.thisObject
        val processRecordKt =
            ProcessRecordKt(
                runningInfo.activityManagerService,
                ProcessStateRecord.getProcessRecord(processStateRecord)
            )
        val appInfo = runningInfo.getRunningAppInfo(processRecordKt.uid)
        // 主进程首次创建时appInfo还未初始化, 此情况无需关心
        appInfo ?: return

        val mPid = try {
            appInfo.getmPid()
        } catch (t: Throwable) {
            Int.MIN_VALUE
        }

        if (processRecordKt.pid == mPid) {
            /*if (BuildConfig.DEBUG) {
                logger.debug("getCurAdj() >>> 包名: ${processRecordKt.packageName}, uid: ${processRecordKt.uid}, pid: ${processRecordKt.pid}, 目标主进程, 给你返回${ProcessRecordKt.DEFAULT_MAIN_ADJ}")
            }*/
            param.result = ProcessRecordKt.DEFAULT_MAIN_ADJ
        }
    }
}