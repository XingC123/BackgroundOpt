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

package com.venus.backgroundopt.xposed.point

import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.point.handler.AndroidHookHandler
import com.venus.backgroundopt.xposed.point.handler.PowerKeeperHookHandler
import com.venus.backgroundopt.xposed.point.handler.SelfHookHandler
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 * @author XingC
 * @date 2024/8/17
 */
class XposedEntry : IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam?) {
        when (lpparam?.packageName) {
            "android" -> AndroidHookHandler(lpparam)
            // miui 电量与性能
            "com.miui.powerkeeper" -> PowerKeeperHookHandler(lpparam)
            // 自己
            BuildConfig.APPLICATION_ID -> SelfHookHandler(lpparam)
        }
    }
}