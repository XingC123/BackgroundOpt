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

import android.content.res.Resources
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/10/13
 */
class ResourcesHook(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.Resources,
                MethodConstants.getInteger,
                arrayOf(
                    beforeHookAction {
                        handleGetInteger(it)
                    }
                ),
                Int::class.javaPrimitiveType
            )
        )
    }

    fun handleGetInteger(param: MethodHookParam) {
        val resources = param.thisObject as Resources
        val resId = param.args[0] as Int
//        val packageName = resources.getResourcePackageName(resId)
//        val typeName = resources.getResourceTypeName(resId)
        val resourceName = resources.getResourceName(resId)
//        val resourceName = resources.getResourceEntryName(resId)  // config_customizedMaxCachedProcesses

//        if (packageName == null || typeName == null || resourceName == null) {
//            return
//        }

        resourceName ?: return

        /**
         * [ActivityManagerConstantsHook.setAMCArgs]
         * 系统无法启动
         */
        if (resourceName == "android:integer/config_customizedMaxCachedProcesses") {
            param.result = Int.MAX_VALUE
        }
    }
}