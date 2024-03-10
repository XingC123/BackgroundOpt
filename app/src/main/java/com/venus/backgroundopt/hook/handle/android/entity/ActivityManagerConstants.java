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
                    
 package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.hook.constants.ClassConstants;

/**
 * 封装了 {@link ClassConstants#ActivityManagerConstants}
 * @author XingC
 * @date 2023/7/21
 */
public class ActivityManagerConstants {
    /**
     * 退到一定时间, 允许使用的最大cpu占比
     */
    // <=5min
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_1 = 25;
    // (5, 10]
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_2 = 25;
    // (10, 15]
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_3 = 10;
    // >15
    private static final int DEFAULT_POWER_CHECK_MAX_CPU_4 = 2;

    public static final String KEY_MAX_CACHED_PROCESSES = "max_cached_processes";
}