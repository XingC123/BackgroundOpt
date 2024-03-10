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