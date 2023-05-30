package com.venus.backgroundopt.server;

import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/20
 */
public class Process {
    private Object process;
    private static Class<?> Process;

    public static Class<?> getProcess(ClassLoader classLoader) {
        if (Process == null) {
            Process = XposedHelpers.findClass(ClassConstants.Process, classLoader);
        }

        return Process;
    }

    public Process(Object process) {
        this.process = process;
    }

    public static int getUidForPid(ClassLoader classLoader, int pid) {
        return (int) XposedHelpers.callStaticMethod(getProcess(classLoader), MethodConstants.getUidForPid, pid);
    }

    public static int getParentPid(ClassLoader classLoader, int pid) {
        return (int) XposedHelpers.callStaticMethod(getProcess(classLoader), MethodConstants.getParentPid, pid);
    }
}
