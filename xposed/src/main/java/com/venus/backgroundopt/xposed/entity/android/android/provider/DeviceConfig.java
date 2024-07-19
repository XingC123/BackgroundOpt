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
                    
package com.venus.backgroundopt.xposed.entity.android.android.provider;

import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;

/**
 * 封装了 {@link ClassConstants#DeviceConfig}
 *
 * @author XingC
 * @date 2023/7/21
 */
public class DeviceConfig {
    public static String NAMESPACE_ACTIVITY_MANAGER = "activity_manager";

    /**
     * Namespace for all activity manager related features that are used at the native level.
     * These features are applied at reboot.
     */
    public static final String NAMESPACE_ACTIVITY_MANAGER_NATIVE_BOOT = "activity_manager_native_boot";

    // Flags stored in the DeviceConfig API.
    public static final String KEY_USE_COMPACTION = "use_compaction";
}