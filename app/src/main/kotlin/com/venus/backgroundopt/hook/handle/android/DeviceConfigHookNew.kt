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
import com.venus.backgroundopt.environment.Features
import com.venus.backgroundopt.hook.base.IHook
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ActivityManagerConstants
import com.venus.backgroundopt.hook.handle.android.entity.DeviceConfig
import com.venus.backgroundopt.utils.beforeHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2024/3/4
 */
class DeviceConfigHookNew(classLoader: ClassLoader?, runningInfo: RunningInfo?) :
    IHook(classLoader, runningInfo) {
    override fun hook() {
        ClassConstants.DeviceConfig.beforeHook(
            classLoader = classLoader,
            methodName = MethodConstants.getBoolean,
            paramTypes = arrayOf(
                String::class.java,
                String::class.java,
                Boolean::class.java,
            )
        ) { param ->
            val namespace = param.args[0] as String
            val key = param.args[1] as String
            if (namespace == DeviceConfig.NAMESPACE_ACTIVITY_MANAGER) {
                when (key) {
                    DeviceConfig.KEY_USE_COMPACTION -> param.result = false
                    ActivityManagerConstants.KEY_USE_TIERED_CACHED_ADJ -> {
                        hookIf(
                            param = param,
                            value = Features.USE_TIERED_CACHED_ADJ
                        ) { Features.USE_TIERED_CACHED_ADJ != null }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun <E> hookIf(param: MethodHookParam, value: E, predicate: () -> Boolean) {
        if (predicate()) {
            param.result = value
        }
    }
}