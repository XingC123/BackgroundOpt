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
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.constants.ServiceConstants
import com.venus.backgroundopt.utils.afterHook
import com.venus.backgroundopt.utils.concurrent.newThreadTask

/**
 * @author XingC
 * @date 2024/2/29
 */
class ServiceManagerHook(
    classLoader: ClassLoader?,
    runningInfo: RunningInfo?
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.ServiceManager.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.addService,
            hookAllMethod = true
        ) { param ->
            val name = param.args[0] as String
            /*
             * 此方法有多个重载方法,
             * public static void addService(String name, Class type)
             * public static void addService(String name, IServiceCreator creator)
             * public static void addService(String name, IBinder service)等
             * 因此对第二个参数直接强转可能会导致ClassCastException
             */
            // val service = param.args[1] as IBinder

            when (name) {
                ServiceConstants.role -> newThreadTask(runningInfo::initActiveDefaultAppPackageName)
            }
        }
    }
}