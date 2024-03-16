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

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.hook.constants.ClassConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/9
 */
public class UidRecord {
    private static Class<?> uidRecordClass;

    public static Class<?> getUidRecordClass(ClassLoader classLoader) {
        if (uidRecordClass == null) {
            uidRecordClass = XposedHelpers.findClass(ClassConstants.UidRecord, classLoader);
        }
        return uidRecordClass;
    }
    @AndroidObject(classPath = ClassConstants.UidRecord)
    private Object uidRecord;
}