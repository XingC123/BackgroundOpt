package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

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

    public boolean hasForegroundActivities() {
        return (boolean) XposedHelpers.callMethod(this.processStateRecord, MethodConstants.hasForegroundActivities);
    }
}
