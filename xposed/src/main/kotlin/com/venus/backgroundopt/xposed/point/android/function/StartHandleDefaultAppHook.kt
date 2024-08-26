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

import com.venus.backgroundopt.common.util.concurrent.newThreadTask
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.SystemServer
import com.venus.backgroundopt.xposed.hook.base.IHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.hook.constants.ServiceConstants
import com.venus.backgroundopt.xposed.util.afterHook

/**
 * @author XingC
 * @date 2024/8/26
 */
class StartHandleDefaultAppHook(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    override fun hook() {
        /*ClassConstants.SystemServer.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.startOtherServices,
            hookAllMethod = true
        ) { _ ->
            newThreadTask(runningInfo::initActiveDefaultAppPackageName)
        }*/

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

        ClassConstants.SystemServiceManager.afterHook(
            classLoader = classLoader,
            methodName = MethodConstants.startService,
            paramTypes = arrayOf(
                Class::class.java
            )
        ) { param ->
            val clazz = param.args[0] as? Class<*> ?: return@afterHook

            when (clazz.canonicalName) {
                SystemServer.ROLE_SERVICE_CLASS -> newThreadTask(runningInfo::initActiveDefaultAppPackageName)
            }
        }
    }
}