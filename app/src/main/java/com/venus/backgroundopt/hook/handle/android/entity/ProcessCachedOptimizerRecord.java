package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

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
}
