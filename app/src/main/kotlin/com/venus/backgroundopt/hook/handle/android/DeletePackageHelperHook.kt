package com.venus.backgroundopt.hook.handle.android

import android.os.UserHandle
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/7/24
 */
class DeletePackageHelperHook(
    classLoader: ClassLoader?,
    hookInfo: RunningInfo?
) : MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
//            HookPoint(
//                ClassConstants.DeletePackageHelper,
//                MethodConstants.deletePackageX,
//                arrayOf(
//                    beforeHookAction {
//                        handleDeletePackageX(it)
//                    }
//                ),
//                String::class.java, /* packageName */
//                Long::class.java,   /* versionCode */
//                Int::class.java,    /* userId */
//                Int::class.java,    /* deleteFlags */
//                Boolean::class.java /* removedBySystem */
//            ),
            HookPoint(
                ClassConstants.DeletePackageHelper,
                MethodConstants.deletePackageLIF,
                arrayOf(
                    beforeHookAction {
                        handleDeletePackageLIF(it)
                    }
                ),
                String::class.java,                 /* packageName */
                UserHandle::class.java,             /* user */
                Boolean::class.java,                /* deleteCodeAndResources */
                IntArray::class.java,               /* allUserHandles */
                Int::class.java,                    /* flags */
                ClassConstants.PackageRemovedInfo,  /* outInfo */
                Boolean::class.java                 /* writeSettings */
            ),
        )
    }

    private fun handleDeletePackageX(param: MethodHookParam) {
        val packageName = param.args[0] as String

        runningInfo.removeAllRecordedNormalApp(packageName)

        if (BuildConfig.DEBUG) {
            logger.debug("卸载: $packageName")
        }
    }

    private fun handleDeletePackageLIF(param: MethodHookParam) {
        val args = param.args
        val packageName = args[0] as String
        val userIds = args[3] as IntArray

        userIds.forEach { userId ->
            runningInfo.removeRecordedNormalApp(userId, packageName)
        }
    }
}