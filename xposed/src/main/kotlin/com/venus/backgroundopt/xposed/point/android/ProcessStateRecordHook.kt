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

import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessStateRecord
import com.venus.backgroundopt.xposed.hook.action.beforeHookAction
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.MethodHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/25
 */
class ProcessStateRecordHook(
    classLoader: ClassLoader,
    hookInfo: RunningInfo
) : MethodHook(classLoader, hookInfo) {
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
        val pid = ProcessStateRecord.getPid(processStateRecord)
        // 主进程首次创建时appInfo还未初始化, 此情况无需关心
        val appInfo = runningInfo.getRunningProcess(pid)?.appInfo?:return

        val mPid = try {
            appInfo.mPid
        } catch (t: Throwable) {
            Int.MIN_VALUE
        }

        if (pid == mPid) {
            if (appInfo.mainProcFixedAdj != ProcessRecord.DEFAULT_MAIN_ADJ) {
                // 放行
            } else {
                param.result = null
            }
        }
    }

    private fun handleGetCurAdj(param: MethodHookParam) {
        val processStateRecord = param.thisObject
        val pid = ProcessStateRecord.getPid(processStateRecord)
        // 主进程首次创建时appInfo还未初始化, 此情况无需关心
        val appInfo = runningInfo.getRunningProcess(pid)?.appInfo?:return

        val mPid = try {
            appInfo.mPid
        } catch (t: Throwable) {
            Int.MIN_VALUE
        }

        if (pid == mPid) {
            /*if (BuildConfig.DEBUG) {
                logger.debug("getCurAdj() >>> 包名: ${processRecordKt.packageName}, uid: ${processRecordKt.uid}, pid: ${processRecordKt.pid}, 目标主进程, 给你返回${ProcessRecordKt.DEFAULT_MAIN_ADJ}")
            }*/
            param.result = ProcessRecord.DEFAULT_MAIN_ADJ
        }
    }
}