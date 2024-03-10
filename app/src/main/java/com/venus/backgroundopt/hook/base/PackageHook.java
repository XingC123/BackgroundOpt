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
                    
 package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.annotation.HookPackageName;
import com.venus.backgroundopt.environment.SystemProperties;
import com.venus.backgroundopt.utils.log.ILogger;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class PackageHook implements ILogger {

    public PackageHook(XC_LoadPackage.LoadPackageParam packageParam) {
        // 环境
        SystemProperties.loadSystemPropertiesClazz(packageParam.classLoader);

        // hook
        hook(packageParam);
    }

    public static String getTargetPackageName(Class<?> aClass) {
        HookPackageName annotation = aClass.getAnnotation(HookPackageName.class);
        String hookPackageName;

        if (annotation == null || "".equals(hookPackageName = annotation.value())) {
            return aClass.getCanonicalName();
        }

        return hookPackageName;
    }

    public abstract void hook(XC_LoadPackage.LoadPackageParam packageParam);
}