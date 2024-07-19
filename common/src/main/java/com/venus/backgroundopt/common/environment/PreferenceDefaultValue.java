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

package com.venus.backgroundopt.common.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy.MainProcessAdjManagePolicy;
import com.venus.backgroundopt.common.entity.message.ForegroundProcTrimMemLevelEnum;
import com.venus.backgroundopt.common.entity.message.GlobalOomScoreEffectiveScopeEnum;
import com.venus.backgroundopt.common.entity.preference.OomWorkModePref;
import com.venus.backgroundopt.common.environment.constants.PreferenceKeyConstants;
import com.venus.backgroundopt.common.environment.constants.PreferenceNameConstants;
import com.venus.backgroundopt.common.util.preference.PreferencesUtilKt;
import com.venus.backgroundopt.xposed.entity.self.ComponentCallbacks2Constants;
import com.venus.backgroundopt.xposed.entity.self.ProcessAdjConstants;

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

    static boolean isEnableBackgroundTrimMem(@NonNull Context context) {
        return PreferencesUtilKt.pref(
                context,
                PreferenceNameConstants.MAIN_SETTINGS
        ).getBoolean(
                PreferenceKeyConstants.ENABLE_BACKGROUND_PROC_TRIM_MEM_POLICY,
                enableBackgroundTrimMem
        );
    }

    /* *************************************************************************
     *                                                                         *
     * OOM工作模式                                                               *
     *                                                                         *
     **************************************************************************/
    int oomWorkMode = OomWorkModePref.MODE_STRICT_SECONDARY;

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
    int customGlobalOomScoreValue = ProcessAdjConstants.FOREGROUND_APP_ADJ;

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
    int backgroundProcMemTrimLevel = ComponentCallbacks2Constants.TRIM_MEMORY_BACKGROUND;

    /* *************************************************************************
     *                                                                         *
     * 划卡杀后台                                                                *
     *                                                                         *
     **************************************************************************/
    boolean killAfterRemoveTask = true;

    /* *************************************************************************
     *                                                                         *
     * 有界面时临时保活主进程                                                      *
     *                                                                         *
     **************************************************************************/
    boolean keepMainProcessAliveHasActivity = true;

    /* *************************************************************************
     *                                                                         *
     * 主进程adj管理策略                                                          *
     *                                                                         *
     **************************************************************************/
     MainProcessAdjManagePolicy mainProcessAdjManagePolicy = MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_HAS_ACTIVITY;

    /* *************************************************************************
     *                                                                         *
     * 次级消息发送器                                                             *
     *                                                                         *
     **************************************************************************/
    boolean enableSecondaryMessageSender = false;
}