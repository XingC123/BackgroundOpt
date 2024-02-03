package com.venus.backgroundopt.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants;
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants;
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
    boolean enableForegroundTrimMem = false;
    boolean enableBackgroundTrimMem = true;
    boolean enableBackgroundGc = false;

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
     * webview进程处理                                                           *
     *                                                                         *
     **************************************************************************/
    boolean enableWebviewProcessProtect = true;
}
