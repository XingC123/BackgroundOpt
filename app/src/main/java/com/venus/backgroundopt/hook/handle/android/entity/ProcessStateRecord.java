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
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.XposedUtilsKt;

import de.robv.android.xposed.XposedHelpers;

/**
 * 对{@link ClassConstants#ProcessStateRecord}的包装
 *
 * @author XingC
 * @date 2023/7/11
 */
public class ProcessStateRecord {
    @AndroidObject(classPath = ClassConstants.ProcessStateRecord)
    private final Object processStateRecord;

    @AndroidObject
    public Object getProcessStateRecord() {
        return processStateRecord;
    }

    public ProcessStateRecord(@AndroidObject Object processStateRecord) {
        this.processStateRecord = processStateRecord;
    }

    @AndroidObject
    public static Object getProcessRecord(@AndroidObject Object processStateRecord) {
        return XposedHelpers.getObjectField(processStateRecord, FieldConstants.mApp);
    }

    public void setCurrentSchedulingGroup(int curSchedGroup) {
        XposedHelpers.callMethod(
                processStateRecord,
                MethodConstants.setCurrentSchedulingGroup,
                curSchedGroup
        );
    }

    public void setSetSchedGroup(int setSchedGroup) {
        XposedHelpers.callMethod(
                processStateRecord,
                MethodConstants.setSetSchedGroup,
                setSchedGroup
        );
    }

    public void setMaxAdj(int maxAdj) {
        XposedHelpers.callMethod(this.processStateRecord, MethodConstants.setMaxAdj, maxAdj);
    }

    public int getMaxAdj() {
        return (int) XposedHelpers.callMethod(this.processStateRecord, MethodConstants.getMaxAdj);
    }

    public void setCurAdj(int curAdj) {
        setCurAdj(processStateRecord, curAdj);
    }

    public int getCurAdj() {
        return getCurAdj(processStateRecord);
    }

    public void setSetAdj(int setAdj) {
        setSetAdj(processStateRecord, setAdj);
    }

    public int getSetAdj() {
        return getSetAdj(processStateRecord);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public int getCurRawAdj() {
        return getCurRawAdj(processStateRecord);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public void setCurRawAdj(int curRawAdj) {
        setCurRawAdj(processStateRecord, curRawAdj);
    }

    public boolean hasForegroundActivities() {
        return (boolean) XposedHelpers.callMethod(this.processStateRecord, MethodConstants.hasForegroundActivities);
    }

    public void setCached(boolean cached) {
        XposedUtilsKt.callMethod(processStateRecord, MethodConstants.setCached, cached);
    }

    public boolean getCached() {
        return (boolean) XposedUtilsKt.callMethod(processStateRecord, MethodConstants.getCached);
    }

    public void setEmpty(boolean empty) {
        XposedUtilsKt.callMethod(processStateRecord, MethodConstants.setEmpty, empty);
    }

    public boolean getEmpty() {
        return (boolean) XposedUtilsKt.callMethod(processStateRecord, MethodConstants.getEmpty);
    }

    public void setCurProcState(int curProcState) {
        XposedUtilsKt.callMethod(processStateRecord, MethodConstants.setCurProcState, curProcState);
    }

    public int getCurProcState() {
        return (int) XposedUtilsKt.callMethod(processStateRecord, MethodConstants.getCurProcState);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public long getLastStateTime() {
        return getLastStateTime(processStateRecord);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public static int getCurAdj(@NonNull @AndroidObject Object instance) {
        return (int) XposedUtilsKt.callMethod(instance, MethodConstants.getCurAdj);
    }

    public static void setCurAdj(@NonNull @AndroidObject Object instance, int curAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setCurAdj, curAdj);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public static int getSetAdj(@NonNull @AndroidObject Object instance) {
        return (int) XposedUtilsKt.callMethod(instance, MethodConstants.getSetAdj);
    }

    public static void setSetAdj(@NonNull @AndroidObject Object instance, int setAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setSetAdj, setAdj);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public static int getCurRawAdj(@NonNull @AndroidObject Object instance) {
        return (int) XposedUtilsKt.callMethod(instance, MethodConstants.getCurRawAdj);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public static void setCurRawAdj(@NonNull @AndroidObject Object instance, int curRawAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setCurRawAdj, curRawAdj);
    }

    public static int getCompletedAdjSeq(@NonNull @AndroidObject Object instance) {
        return (int) XposedUtilsKt.callMethod(instance, MethodConstants.getCompletedAdjSeq);
    }

    @AndroidMethod(classPath = ClassConstants.ProcessStateRecord)
    public static long getLastStateTime(@NonNull @AndroidObject Object instance) {
        return (long) XposedUtilsKt.callMethod(instance, MethodConstants.getLastStateTime);
    }
}