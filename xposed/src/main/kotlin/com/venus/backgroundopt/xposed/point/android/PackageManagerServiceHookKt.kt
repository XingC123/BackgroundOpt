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

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.UserHandle
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.action.afterHookAction
import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.MethodHook
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.hook.effectiveHookFlagMaker
import com.venus.backgroundopt.xposed.hook.generateHookPoint
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

/**
 * @author XingC
 * @date 2023/7/25
 */
class PackageManagerServiceHookKt(
    classLoader: ClassLoader,
    hookInfo: RunningInfo,
) : MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            /*generateHookPoint(
                effectiveHookFlagMaker {
                    true
                },
                ClassConstants.PackageManagerService,
                MethodConstants.isFirstBoot,
                arrayOf(
                    afterHookAction {
                        getActiveLaunchPackageName(it)
                    }
                ),
            ),*/
            /**
             * 安卓12才进行hook
             * 安卓13已经更换实现。模块其实现位于 [DeletePackageHelperHook.handleDeletePackageLIF]
             */
            generateHookPoint(
                effectiveHookFlagMaker {
                    VERSION.SDK_INT in VERSION_CODES.S..VERSION_CODES.S_V2
                },
                ClassConstants.PackageManagerService,
                MethodConstants.deletePackageLIF,
                arrayOf(
                    afterHookAction {
                        handleDeletePackageLIF(it)
                    }
                ),
                String::class.java,                     /* packageName */
                UserHandle::class.java,                 /* user */
                Boolean::class.java,                    /* deleteCodeAndResources */
                IntArray::class.java,                   /* allUserHandles */
                Int::class.java,                        /* flags */
                ClassConstants.PackageRemovedInfo_A12,  /* outInfo */
                Boolean::class.java,                    /* writeSettings */
                ClassConstants.ParsedPackage            /* replacingPackage */
            ),
        )
    }

    private fun getActiveLaunchPackageName(param: MethodHookParam) {
        // 在ActivityManagerService加载完毕后再获取
        runningInfo.activityManagerService ?: return

        // 若已获取默认桌面的包名, 则不进行任何操作
        runningInfo.activeLaunchPackageName?.let { return }

        try {
            val mPackageManagerService = param.thisObject
            val mInjector =
                XposedHelpers.getObjectField(mPackageManagerService, FieldConstants.mInjector)
            val mDefaultAppProvider =
                XposedHelpers.callMethod(mInjector, MethodConstants.getDefaultAppProvider)

            val packageName = XposedHelpers.callMethod(
                mDefaultAppProvider,
                MethodConstants.getDefaultHome,
                0
            ) as String

            // runningInfo.activeLaunchPackageName = packageName
            if (BuildConfig.DEBUG) {
                logger.debug("默认启动器为: $packageName")
            }
        } catch (ignore: Throwable) {
        }
    }

    private fun handleDeletePackageLIF(param: MethodHookParam) {
        val args: Array<Any> = param.args
        val packageName = args[0] as String
        val userIds = args[3] as IntArray

        userIds.forEach { userId ->
            runningInfo.removeRecordedFindAppResult(userId, packageName)
        }
    }
}