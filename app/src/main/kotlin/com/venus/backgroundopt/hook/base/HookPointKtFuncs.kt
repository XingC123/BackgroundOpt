package com.venus.backgroundopt.hook.base

import com.venus.backgroundopt.hook.base.action.HookAction

/**
 * @author XingC
 * @date 2023/7/25
 */

/**
 * 生成 [HookPoint]
 * @param effectiveFlag 当此值为true时, 才真正进行hook
 * @param className 要hook的类的全路径
 * @param methodName 要hook的方法的名称
 * @param hookAction hook行为
 * @param actionArgs 要hook的发方法所需参数的Class。如果方法为空参, 则不需要传递此值
 */
fun generateHookPoint(
    effectiveFlag: Boolean,
    className: String,
    methodName: String,
    hookAction: Array<HookAction>,
    vararg actionArgs: Any
): HookPoint {
    return if (effectiveFlag) {
        HookPoint(className, methodName, hookAction, *actionArgs)
    } else {
        IneffectiveHookPoint(className, methodName)
    }
}

/**
 * 生成 [HookPoint]
 * 参数含义详见 [generateHookPoint]
 */
fun generateHookPoint(
    effectiveFlag: Boolean,
    className: String,
    hookAction: Array<HookAction>,
    vararg actionArgs: Any
): HookPoint {
    return if (effectiveFlag) {
        HookPoint(className, hookAction, *actionArgs)
    } else {
        IneffectiveHookPoint(className)
    }
}