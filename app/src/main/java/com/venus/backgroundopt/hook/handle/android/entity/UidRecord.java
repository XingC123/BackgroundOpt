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
