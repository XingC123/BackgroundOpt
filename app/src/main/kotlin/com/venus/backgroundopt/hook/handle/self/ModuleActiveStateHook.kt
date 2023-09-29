package com.venus.backgroundopt.hook.handle.self

import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.base.generateHookPoint
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import kotlin.reflect.jvm.javaGetter

/**
 * @author XingC
 * @date 2023/9/29
 */
class ModuleActiveStateHook(classLoader: ClassLoader?) : MethodHook(classLoader) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            generateHookPoint(
                CommonProperties::class.qualifiedName != null,
                CommonProperties::class.qualifiedName!!,
                CommonProperties::moduleActive.javaGetter!!.name,
                arrayOf(
                    beforeHookAction { handleModuleActiveState(it) }
                )
            )
        )
    }

    /**
     * 改变模块激活状态
     *
     * @param param
     */
    private fun handleModuleActiveState(param: MethodHookParam) {
        param.result = true
    }
}