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

import com.venus.backgroundopt.xposed.core.AppGroupEnum
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.action.afterHookAction
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.MethodHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/22
 */
class ProcessListHookKt(
    classLoader: ClassLoader,
    hookInfo: RunningInfo,
) : MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            /*generateMatchedMethodHookPoint(
                true,
                ClassConstants.ProcessList,
                MethodConstants.removeLruProcessLocked,
                arrayOf(
                    beforeHookAction { handleRemoveLruProcessLocked(it) }
                )
            ),*/
            HookPoint(
                ClassConstants.ProcessList,
                MethodConstants.handleProcessStartedLocked,
                arrayOf(
                    afterHookAction { handleHandleProcessStartedLocked(it) }
                ),
                ClassConstants.ProcessRecord,       // app
                Int::class.javaPrimitiveType,       // pid
                Boolean::class.javaPrimitiveType,   // usingWrapper
                Long::class.javaPrimitiveType,      // expectedStartSeq
                Boolean::class.javaPrimitiveType,   // procAttached
            ),
        )
    }

    /* *************************************************************************
     *                                                                         *
     * 进程新建                                                                  *
     *                                                                         *
     **************************************************************************/
    private fun handleHandleProcessStartedLocked(param: MethodHookParam) {
        if (!(param.result as Boolean)) {
            return
        }

        val proc = param.args[0]
        val pid = param.args[1] as Int
        runningInfo.startProcess(proc, pid)

        /*val proc = param.args[0]
        val uid = ProcessRecord.getUID(proc)
        /*if (ActivityManagerService.isUnsafeUid(uid)) {
            return
        }*/

        val pid = param.args[1] as Int
        val userId = ProcessRecord.getUserId(proc)
        val packageName = ProcessRecord.getPkgName(proc)
        runningInfo.startProcess(proc, uid, userId, packageName, pid)*/
    }

    companion object {
        @JvmField
        val processedAppGroup = arrayOf(
            AppGroupEnum.NONE,
            AppGroupEnum.ACTIVE,
            AppGroupEnum.IDLE
        )
    }
}

