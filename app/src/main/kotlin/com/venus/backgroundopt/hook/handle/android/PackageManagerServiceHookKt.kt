package com.venus.backgroundopt.hook.handle.android

import android.os.Build
import android.os.UserHandle
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.afterHookAction
import com.venus.backgroundopt.hook.base.effectiveHookFlagMaker
import com.venus.backgroundopt.hook.base.generateHookPoint
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

/**
 * @author XingC
 * @date 2023/7/25
 */
class PackageManagerServiceHookKt(
    classLoader: ClassLoader?,
    hookInfo: RunningInfo?
) : MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            generateHookPoint(
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
            ),
            /**
             * 安卓12才进行hook
             * 安卓13已经更换实现。模块其实现位于 [DeletePackageHelperHook.handleDeletePackageLIF]
             */
            generateHookPoint(
                effectiveHookFlagMaker {
                    Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2
                },
                ClassConstants.PackageManagerService,
                MethodConstants.deletePackageLIF,
                arrayOf(
                    afterHookAction {
                        handleDeletePackageLIF(it)
                    }
                ),
                String::class.java, /* packageName */
                UserHandle::class.java, /* user */
                Boolean::class.java,    /* deleteCodeAndResources */
                IntArray::class.java,   /* allUserHandles */
                Int::class.java,    /* flags */
                ClassConstants.PackageRemovedInfo_A12,  /* outInfo */
                Boolean::class.java,    /* writeSettings */
                ClassConstants.ParsedPackage /* replacingPackage */
            ),
        )
    }

    private fun getActiveLaunchPackageName(param: MethodHookParam) {
        // 在ActivityManagerService加载完毕后再获取
        if (runningInfo.activityManagerService == null) {
            return
        }

        // 若已获取默认桌面的包名, 则不进行任何操作
        if (runningInfo.activeLaunchPackageName != null) {
            return
        }

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

        if (BuildConfig.DEBUG) {
            logger.debug("默认启动器为: $packageName")
        }

        runningInfo.activeLaunchPackageName = packageName
    }

    private fun handleDeletePackageLIF(param: MethodHookParam) {
        val args: Array<Any> = param.args
        val packageName = args[0] as String
        val userIds = args[3] as IntArray
        val runningInfo = runningInfo

        for (userId in userIds) {
            runningInfo.removeRecordedNormalApp(runningInfo.getNormalAppKey(userId, packageName))
        }
    }
}