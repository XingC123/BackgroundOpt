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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.xposed.annotation.OriginalObject;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;
import com.venus.backgroundopt.xposed.util.XposedUtilsKt;

/**
 * 封装了 {@link ClassConstants#ProcessCachedOptimizerRecord}
 *
 * @author XingC
 * @date 2023/7/13
 */
public class ProcessCachedOptimizerRecord {
    @OriginalObject(classPath = ClassConstants.ProcessCachedOptimizerRecord)
    private final Object processCachedOptimizerRecord;

    public ProcessCachedOptimizerRecord(@OriginalObject Object processCachedOptimizerRecord) {
        this.processCachedOptimizerRecord = processCachedOptimizerRecord;
    }

    public void setReqCompactAction(int reqCompactAction) {
        XposedUtilsKt.callMethod(
                this.processCachedOptimizerRecord,
                MethodConstants.setReqCompactAction,
                reqCompactAction
        );
    }

    @OriginalObject(classPath = ClassConstants.ProcessCachedOptimizerRecord)
    public boolean isFreezeExempt() {
        return isFreezeExempt(processCachedOptimizerRecord);
    }

    @OriginalObject(classPath = ClassConstants.ProcessCachedOptimizerRecord)
    public static boolean isFreezeExempt(@NonNull @OriginalObject Object instance) {
        return (boolean) XposedUtilsKt.callMethod(instance, MethodConstants.isFreezeExempt);
    }
}