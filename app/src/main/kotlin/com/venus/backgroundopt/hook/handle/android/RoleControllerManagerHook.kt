package com.venus.backgroundopt.hook.handle.android

import android.app.role.RoleManager
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/8/27
 */
class RoleControllerManagerHook(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.RoleControllerManager,
                MethodConstants.onAddRoleHolder,
                arrayOf(
                    beforeHookAction {
                        handleOnAddRoleHolder(it)
                    }
                ),
                String::class.java,             /* roleName */
                String::class.java,             /* packageName */
                Int::class.java,                /* flags */
                ClassConstants.RemoteCallback   /* callback */
            )
        )
    }

    private fun handleOnAddRoleHolder(param: MethodHookParam) {
        when (param.args[0] as String) {
            RoleManager.ROLE_HOME -> {
                val packageName = param.args[1] as String
                runningInfo.activeLaunchPackageName = packageName
                logger.debug("更换的桌面包名为: $packageName")
            }
        }
    }
}