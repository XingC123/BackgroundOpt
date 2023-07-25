package com.venus.backgroundopt.hook.base.action

import de.robv.android.xposed.XC_MethodHook.MethodHookParam

fun beforeHookAction(action: (p: MethodHookParam) -> Any?): BeforeHookAction {
    return BeforeHookAction { param ->
        action(param)
        null
    }
}

fun afterHookAction(action: (p: MethodHookParam) -> Any?): AfterHookAction {
    return AfterHookAction { param ->
        action(param)
        null
    }
}

fun replacementHookAction(action: (p: MethodHookParam) -> Any?): ReplacementHookAction {
    return ReplacementHookAction { param -> action(param) }
}
