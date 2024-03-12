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
                    
 package com.venus.backgroundopt.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.entity.preference.OomWorkModePref;
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants;
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants;
import com.venus.backgroundopt.hook.handle.android.entity.ComponentCallbacks2;
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList;
import com.venus.backgroundopt.utils.message.handle.ForegroundProcTrimMemLevelEnum;
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeEnum;
import com.venus.backgroundopt.utils.preference.PreferencesUtilKt;

/**
 * @author XingC
 * @date 2024/2/1
 */
public interface PreferenceDefaultValue {
    /* *************************************************************************
     *                                                                         *
     * app内存优化策略                                                           *
     *                                                                         *
     **************************************************************************/
    boolean enableForegroundTrimMem = true;
    boolean enableBackgroundTrimMem = true;
    boolean enableBackgroundGc = true;

    static boolean isEnableForegroundTrimMem(@NonNull Context context) {
        return PreferencesUtilKt.pref(
                context,
                PreferenceNameConstants.MAIN_SETTINGS
        ).getBoolean(
                PreferenceKeyConstants.ENABLE_FOREGROUND_PROC_TRIM_MEM_POLICY,
                enableForegroundTrimMem
        );
    }

    /* *************************************************************************
     *                                                                         *
     * OOM工作模式                                                               *
     *                                                                         *
     **************************************************************************/
    int oomWorkMode = OomWorkModePref.MODE_STRICT;

    /* *************************************************************************
     *                                                                         *
     * webview进程处理                                                           *
     *                                                                         *
     **************************************************************************/
    boolean enableWebviewProcessProtect = true;

    /* *************************************************************************
     *                                                                         *
     * Simple Lmk                                                              *
     *                                                                         *
     **************************************************************************/
    boolean enableSimpleLmk = true;

    /* *************************************************************************
     *                                                                         *
     * 全局OOM                                                                  *
     *                                                                         *
     **************************************************************************/
    boolean enableGlobalOomScore = false;
    String globalOomScoreEffectiveScopeName = GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS.name();
    int customGlobalOomScoreValue = ProcessList.FOREGROUND_APP_ADJ;

    /* *************************************************************************
     *                                                                         *
     * 前台内存回收                                                              *
     *                                                                         *
     **************************************************************************/
    String foregroundProcTrimMemLevelEnumName = ForegroundProcTrimMemLevelEnum.RUNNING_MODERATE.name();

    /* *************************************************************************
     *                                                                         *
     * 后台内存回收                                                              *
     *                                                                         *
     **************************************************************************/
    int backgroundProcMemTrimLevel = ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
}