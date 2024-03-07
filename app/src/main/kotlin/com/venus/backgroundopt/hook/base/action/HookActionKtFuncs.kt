package com.venus.backgroundopt.hook.base.action

import com.venus.backgroundopt.utils.log.logError
import com.venus.backgroundopt.utils.runCatchThrowable
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

fun beforeHookAction(action: (p: MethodHookParam) -> Any?): BeforeHookAction {
    return BeforeHookAction { param ->
        runCatchThrowable(catchBlock = {
            logError(logStr = "beforeHookAction出错", t = it)
        }) {
            action(param)
        }
        null
    }
}

fun afterHookAction(action: (p: MethodHookParam) -> Any?): AfterHookAction {
    return AfterHookAction { param ->
        runCatchThrowable(catchBlock = {
            logError(logStr = "afterHookAction出错", t = it)
        }) {
            action(param)
        }
        null
    }
}

fun replacementHookAction(action: (p: MethodHookParam) -> Any?): ReplacementHookAction {
    return ReplacementHookAction { param -> action(param) }
}

fun doNothingHookAction():DoNotingHookAction {
    return DoNotingHookAction()
}