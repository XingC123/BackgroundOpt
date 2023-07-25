package com.venus.backgroundopt.hook.base

/**
 * @author XingC
 * @date 2023/7/25
 */
class IneffectiveHookPoint(
    className: String,
    methodName: String?
) : HookPoint(
    className,
    methodName,
    null,
    null
) {
    constructor(className: String) : this(className, null)
}