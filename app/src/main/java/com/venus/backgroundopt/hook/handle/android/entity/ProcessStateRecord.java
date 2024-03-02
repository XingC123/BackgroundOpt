package com.venus.backgroundopt.hook.handle.android.entity;

import androidx.annotation.NonNull;

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
        return XposedUtilsKt.getIntFieldValue(processStateRecord, MethodConstants.getCurAdj);
    }

    public void setSetAdj(int setAdj) {
        setSetAdj(processStateRecord, setAdj);
    }

    public int getSetAdj() {
        return XposedUtilsKt.getIntFieldValue(processStateRecord, MethodConstants.getSetAdj);
    }

    public boolean hasForegroundActivities() {
        return (boolean) XposedHelpers.callMethod(this.processStateRecord, MethodConstants.hasForegroundActivities);
    }

    public static void setCurAdj(@NonNull @AndroidObject Object instance, int curAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setCurAdj, curAdj);
    }

    public static void setSetAdj(@NonNull @AndroidObject Object instance, int setAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setSetAdj, setAdj);
    }
}
