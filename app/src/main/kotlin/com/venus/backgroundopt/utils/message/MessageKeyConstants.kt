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
                    
 package com.venus.backgroundopt.utils.message

/**
 * ui与模块所在进程通信所使用的消息标识
 *
 * @author XingC
 * @date 2023/9/21
 */
interface MessageKeyConstants {
    companion object {
        const val getRunningAppInfo = "getRunningAppInfo"
        const val getTargetAppGroup = "getTargetAppGroup"
        const val getBackgroundTasks = "getBackgroundTasks"
        const val getAppCompactList = "getAppCompactList"
        const val subProcessOomConfigChange = "subProcessOomConfigChange"
        const val getInstalledApps = "getInstalledApps"
        const val autoStopCompactTask = "autoStopCompactTask"
        const val enableForegroundProcTrimMemPolicy = "enableForegroundProcTrimMemPolicy"
        const val foregroundProcTrimMemPolicy = "foregroundProcTrimMemPolicy"
        const val appOptimizePolicy = "appOptimizePolicy"
        const val appWebviewProcessProtect = "appWebviewProcessProtect"
        const val enableSimpleLmk = "enableSimpleLmk"
        const val enableGlobalOomScore = "enableGlobalOomScore"
        const val globalOomScoreEffectiveScope = "globalOomScoreEffectiveScope"
        const val globalOomScoreValue = "globalOomScoreValue"
        const val getTrimMemoryOptThreshold = "getTrimMemoryOptThreshold"
        const val getHomePageModuleInfo = "getHomePageModuleInfo"
        const val killAfterRemoveTask = "killAfterRemoveTask"
        const val moduleRunning = "moduleRunning"
        const val getManagedAdjDefaultApps = "getManagedAdjDefaultApps"
        const val KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY = "KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY"
    }
}