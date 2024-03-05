package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.hook.constants.ClassConstants;

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
