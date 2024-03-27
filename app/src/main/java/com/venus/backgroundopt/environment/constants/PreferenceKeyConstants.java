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
                    
 package com.venus.backgroundopt.environment.constants;

/**
 * @author XingC
 * @date 2023/9/27
 */
public interface PreferenceKeyConstants {
    String AUTO_STOP_COMPACT_TASK = "pref_key_autoStopCompactTask";
    String ENABLE_FOREGROUND_PROC_TRIM_MEM_POLICY = "pref_key_enable_foreground_proc_trim_mem_policy";
    String FOREGROUND_PROC_TRIM_MEM_POLICY = "pref_key_foreground_proc_trim_mem_policy";
    String OOM_WORK_MODE = "pref_key_oom_work_mode";
    String STRICT_OOM_MODE = "pref_key_strict_oom_mode";
    String APP_WEBVIEW_PROCESS_PROTECT = "pref_key_app_webview_process_protect";
    String SIMPLE_LMK = "pref_key_simple_lmk";
    String GLOBAL_OOM_SCORE = "pref_key_global_oom_score";
    String GLOBAL_OOM_SCORE_EFFECTIVE_SCOPE = "pref_key_global_oom_score_effective_scope";
    String GLOBAL_OOM_SCORE_VALUE = "pref_key_global_oom_score_value";
    String KILL_AFTER_REMOVE_TASK = "pref_key_kill_after_remove_task";
    String DYNAMIC_THEME = "pref_key_dynamic_theme";
}