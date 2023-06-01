package com.venus.backgroundopt.environment;

import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/28
 */
public class SystemProperties {
    private static Class<?> SystemPropertiesClazz;

    public static final String venusPrefix = "venus_";

    private static Class<?> getSystemPropertiesClazz() {
        if (SystemPropertiesClazz == null) {
            SystemPropertiesClazz = XposedHelpers.findClass(ClassConstants.SystemProperties, SystemProperties.class.getClassLoader());
        }

        return SystemPropertiesClazz;
    }

    /**
     * 获取指定key的值
     * @param key 键
     */
    public static String getString(String key) {
        return (String) XposedHelpers.callStaticMethod(
                SystemPropertiesClazz, "get", new Class[]{String.class}, key);
    }

    public static String getString(String key, String defaultValue) {
        return (String) XposedHelpers.callStaticMethod(
                SystemPropertiesClazz, "get",
                new Class[]{String.class, String.class}, key, defaultValue);
    }

    public static String generateVenusKey(String identify) {
        return venusPrefix + identify;
    }

    /**
     * 获取Venus软件默认的值
     * @param key 元素名
     * @return 值
     */
    public static String getVenusString(String key) {
        return getString(venusPrefix + key);
    }

    public static String getVenusString(String key, String defaultValue) {
        return getString(venusPrefix + key, defaultValue);
    }

    public static void loadSystemPropertiesClazz(ClassLoader classLoader) {
        SystemPropertiesClazz = XposedHelpers.findClass(ClassConstants.SystemProperties, classLoader);
    }

    public static void set(String key, String value) {
        XposedHelpers.callStaticMethod(getSystemPropertiesClazz(), MethodConstants.set, key, value);
    }
}
