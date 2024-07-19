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
                    
package com.venus.backgroundopt.xposed.entity.android.android.os;

import com.venus.backgroundopt.common.util.log.ILogger;
import com.venus.backgroundopt.xposed.BuildConfig;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;

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
        return SystemPropertiesClazz;
    }

    /**
     * 获取指定key的值
     *
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
     *
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
        try {
            SystemPropertiesClazz = XposedHelpers.findClass(ClassConstants.SystemProperties, classLoader);
        } catch (Throwable ignore) {

        }
    }

    public static void set(String key, String value) {
        Class<?> propertiesClazz = getSystemPropertiesClazz();
        if (propertiesClazz == null) {
            if (BuildConfig.DEBUG) {
                ILogger.getLoggerStatic(SystemProperties.class).warn("未找到: SystemProperties。无法设置: " + key + ", value: " + value);
            }
            return;
        }
        XposedHelpers.callStaticMethod(propertiesClazz, MethodConstants.set, key, value);
    }
}