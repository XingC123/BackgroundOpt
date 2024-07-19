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
                    
package com.venus.backgroundopt.xposed.hook

import com.venus.backgroundopt.xposed.hook.base.HookPoint
import com.venus.backgroundopt.xposed.hook.base.action.HookAction
import com.venus.backgroundopt.common.util.concurrent.ConcurrentHashSet

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
 * hook与给定方法名相同的所有方法
 *
 * 参数含义见[generateHookPoint]
 */
fun generateMatchedMethodHookPoint(
    effectiveFlag: Boolean,
    className: String,
    methodName: String,
    hookAction: Array<HookAction>
): HookPoint {
    return generateHookPoint(
        effectiveFlag,
        className,
        methodName,
        hookAction,
        ConcurrentHashSet.any
    ).apply {
        isHookAllMatchedMethod = true
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