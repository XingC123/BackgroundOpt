package com.venus.backgroundopt.hook.handle.android.entity;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;
import com.venus.backgroundopt.utils.XposedUtilsKt;

import org.jetbrains.annotations.NotNull;

import de.robv.android.xposed.XposedHelpers;

/**
 * 对{@link ClassConstants#ProcessStateRecord}的包装
 *
 * @author XingC
 * @date 2023/7/11
 */
public class ProcessStateRecord {
    private final Object processStateRecord;

    public Object getProcessStateRecord() {
        return processStateRecord;
    }

    public ProcessStateRecord(Object processStateRecord) {
        this.processStateRecord = processStateRecord;
    }

    public static Object getProcessRecord(Object processStateRecord) {
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
        setCurAdj(this, curAdj);
    }

    public int getCurAdj() {
        return XposedUtilsKt.getIntFieldValue(this, MethodConstants.getCurAdj);
    }

    public void setSetAdj(int setAdj) {
        setSetAdj(this, setAdj);
    }

    public int getSetAdj() {
        return XposedUtilsKt.getIntFieldValue(this, MethodConstants.getSetAdj);
    }

    public boolean hasForegroundActivities() {
        return (boolean) XposedHelpers.callMethod(this.processStateRecord, MethodConstants.hasForegroundActivities);
    }

    public static void setCurAdj(@NonNull Object instance, int curAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setCurAdj, curAdj);
    }

    public static void setSetAdj(@NonNull Object instance, int setAdj) {
        XposedUtilsKt.callMethod(instance, MethodConstants.setSetAdj, setAdj);
    }
}
