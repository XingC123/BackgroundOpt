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