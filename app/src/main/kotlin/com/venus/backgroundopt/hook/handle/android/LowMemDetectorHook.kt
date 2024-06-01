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
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.afterConstructorHook
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.replaceHook
import com.venus.backgroundopt.utils.setBooleanFieldValue
import com.venus.backgroundopt.utils.setIntFieldValue

/**
 * @author XingC
 * @date 2024/1/31
 */
class LowMemDetectorHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo
) : IHook(classLoader, runningInfo) {
    @Volatile
    var lowMemDetectorInstance: Any? = null

    override fun hook() {
        // 设置LowMemDetector中部分参数
        fun setLowMemDetectorParam() {
            lowMemDetectorInstance?.let { instance ->
                instance.setIntFieldValue(FieldConstants.mPressureState, ADJ_MEM_FACTOR_NORMAL)
                instance.setBooleanFieldValue(FieldConstants.mAvailable, true)
            }
        }

        ClassConstants.LowMemDetector.afterConstructorHook(
            classLoader = classLoader,
            hookAllMethod = true
        ) { param ->
            lowMemDetectorInstance = param.thisObject
            setLowMemDetectorParam()
        }

        ClassConstants.LowMemDetector.replaceHook(
            classLoader = classLoader,
            methodName = MethodConstants.getMemFactor,
            hookAllMethod = true,
        ) { ADJ_MEM_FACTOR_NORMAL }

        ClassConstants.LowMemDetector.replaceHook(
            classLoader = classLoader,
            methodName = MethodConstants.isAvailable,
            hookAllMethod = true,
        ) { true }

        // 禁止psi检查
        // 此方式会造成锁屏下持续唤醒
        // 测试环境: 红米k30p(lmi) a12 miui13 22.7.8
        // 测试时长: 10min
        /*ClassConstants.LowMemThread.beforeHook(
            classLoader = classLoader,
            methodName = "run"
        ) { param ->
            param.result = null
        }*/

        // hook waitForPressure使其返回-1
        // 通过此方式亦可结束LowMemThread线程
        // 此方式亦会造成锁屏下持续唤醒
        // 所以只要停止LowMemThread就会造成持续唤醒?
        // 以下是LowMemDetector.run()中关于waitForPressure返回-1的逻辑
        //int newPressureState = waitForPressure();
        //if (newPressureState == -1) {
        //    // epoll broke, tear this down
        //    mAvailable = false;
        //    break;
        //}
        /*ClassConstants.LowMemDetector.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.waitForPressure
        ) { it.result = -1 }*/

        ClassConstants.LowMemThread.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.run
        ) {
            setLowMemDetectorParam()
        }
    }

    companion object {
        // com.android.internal.app.procstats.ProcessStats.ADJ_MEM_FACTOR_NORMAL
        const val ADJ_MEM_FACTOR_NORMAL = 0
    }
}