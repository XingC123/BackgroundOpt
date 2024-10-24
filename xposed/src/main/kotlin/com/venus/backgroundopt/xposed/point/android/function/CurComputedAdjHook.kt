/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.xposed.point.android.function

import com.venus.backgroundopt.xposed.annotation.FunctionHook
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessStateRecord
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.beforeHook
import com.venus.backgroundopt.xposed.util.findClass

/**
 * @author XingC
 * @date 2024/10/21
 */
@FunctionHook("计算进程的adj后进行保存")
class CurComputedAdjHook(classLoader: ClassLoader, runningInfo: RunningInfo) :
    IHook(classLoader, runningInfo) {
    override fun hook() {
        val oomAdjusterClazz = ClassConstants.OomAdjuster.findClass(classLoader)
        /*
         * 由于执行顺序的问题, 此方法会更改curRawAdj到进程的maxAdj。
         * 因此需要在此之前获取之前计算好的值。
         * 但在a14的后期版本中, 也存在此方法: https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r74/services/core/java/com/android/server/am/OomAdjuster.java
         */
        val hasSpecialMethod = oomAdjusterClazz.declaredMethods.find {
            it.name == MethodConstants.setIntermediateAdjLSP
        } != null
        if (hasSpecialMethod) {
            ClassConstants.OomAdjuster.beforeHook(
                classLoader = classLoader,
                methodName = MethodConstants.setIntermediateAdjLSP,
                hookAllMethod = true
            ) { param ->
                val app = param.args[0]
                setRawAdj(app)
            }

            ProcessStateRecord.curComputedAdjGetter = { processStateRecord ->
                val curRawAdj = processStateRecord.curRawAdj
                if (curRawAdj == processStateRecord.processRecord.recordMaxAdj) {
                    processStateRecord.originalCurRawAdj
                } else {
                    curRawAdj
                }
            }
        } else {
            ProcessStateRecord.curComputedAdjGetter = { processStateRecord ->
                processStateRecord.curRawAdj
            }
        }
    }

    /**
     * 从 [runningInfo]中, 根据[process]找到包装后的[ProcessRecord], 并记录当前的 curRawAdj
     */
    private fun setRawAdj(process: Any): Boolean {
        val processRecord = runningInfo.getRunningProcess(process) ?: return false

        val processStateRecord = processRecord.processStateRecord
        processStateRecord.curComputedAdj = processStateRecord.curRawAdj

        return true
    }
}