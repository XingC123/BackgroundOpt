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

import androidx.annotation.NonNull;

import com.venus.backgroundopt.annotation.AndroidMethod;
import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.XposedUtilsKt;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了 {@link ClassConstants#ProcessCachedOptimizerRecord}
 *
 * @author XingC
 * @date 2023/7/13
 */
public class ProcessCachedOptimizerRecord {
    @AndroidObject(classPath = ClassConstants.ProcessCachedOptimizerRecord)
    private final Object processCachedOptimizerRecord;

    public ProcessCachedOptimizerRecord(@AndroidObject Object processCachedOptimizerRecord) {
        this.processCachedOptimizerRecord = processCachedOptimizerRecord;
    }

    public void setReqCompactAction(int reqCompactAction) {
        XposedHelpers.callMethod(
                this.processCachedOptimizerRecord,
                MethodConstants.setReqCompactAction,
                reqCompactAction
        );
    }

    @AndroidMethod(classPath = ClassConstants.ProcessCachedOptimizerRecord)
    public boolean isFreezeExempt() {
        return isFreezeExempt(processCachedOptimizerRecord);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessCachedOptimizerRecord)
    public static boolean isFreezeExempt(@NonNull @AndroidObject Object instance) {
        return (boolean) XposedUtilsKt.callMethod(instance, MethodConstants.isFreezeExempt);
    }
}