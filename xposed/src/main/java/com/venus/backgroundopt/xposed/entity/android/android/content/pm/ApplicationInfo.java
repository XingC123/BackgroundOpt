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
                    
package com.venus.backgroundopt.xposed.entity.android.android.content.pm;

import com.venus.backgroundopt.xposed.annotation.OriginalObject;

/**
 * 封装了{@link android.content.pm.ApplicationInfo}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/14
 */
public class ApplicationInfo {
    @OriginalObject(clazz = android.content.pm.ApplicationInfo.class)
    private final android.content.pm.ApplicationInfo applicationInfo;

    @OriginalObject
    public android.content.pm.ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public ApplicationInfo(android.content.pm.ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
        this.uid = applicationInfo.uid;
    }

    public int uid;

    public String getPackageName() {
        return applicationInfo.packageName;
    }
}