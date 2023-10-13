package com.venus.backgroundopt.hook.base

import com.venus.backgroundopt.hook.base.action.HookAction

/**
 * @author XingC
 * @date 2023/7/25
 */

/**
 * 生成 [HookPoint]
 *
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
 * 生抽hook构造函数的[HookPoint]
 *
 * @param effectiveFlag 当此值为true时, 才真正进行hook
 * @param className 要hook的类的全路径
 * @param hookAction hook行为
 * @param actionArgs 要hook的发方法所需参数的Class。如果方法为空参, 则不需要传递此值
 * @return
 */
fun generateConstructorHookPointer(
    effectiveFlag: Boolean,
    className: String,
    hookAction: Array<HookAction>,
    vararg actionArgs: Any
): HookPoint {
    return if (effectiveFlag) {
        HookPoint(className, hookAction, *actionArgs)
    } else {
        IneffectiveHookPoint(className, className)
    }
}

/**
 * 使用 [generateHookPoint] 生成 [HookPoint] 时, 需要传递 effectiveFlag 参数, 该参数是一个Boolean值。可以通过当前
 * 方法，传入一个表达式来计算出一个Boolean进行返回
 *
 * @param action 决定当前方法返回值的表达式
 * @return 返回 [action] 运算后的值
 */
inline fun effectiveHookFlagMaker(action: () -> Boolean): Boolean {
    return action()
}