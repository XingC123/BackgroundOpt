package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/20
 */
public class Process {
    private Object process;

    public static Class<?> getProcess() {
        return android.os.Process.class;
    }

    public Process(Object process) {
        this.process = process;
    }

    public static int getUidForPid(int pid) {
        return (int) XposedHelpers.callStaticMethod(getProcess(), MethodConstants.getUidForPid, pid);
    }

    public static int getParentPid(int pid) {
        return (int) XposedHelpers.callStaticMethod(getProcess(), MethodConstants.getParentPid, pid);
    }

    /**
     * Send a signal to the given process.
     *
     * @param pid    The pid of the target process.
     * @param signal The signal to send.
     */
    public static void sendSignal(int pid, int signal) {
        XposedHelpers.callStaticMethod(
                Process.getProcess(),
                MethodConstants.sendSignal,
                pid,
                signal
        );
    }

    public static long getFreeMemory() {
        return (long) XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.getFreeMemory
        );
    }

    public static long getTotalMemory() {
        return (long) XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.getTotalMemory
        );
    }

    public static void readProcLines(String path, String[] reqFields, long[] outSizes) {
        XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.readProcLines,
                path,
                reqFields,
                outSizes
        );
    }

    /**
     * Adjust the swappiness level for a process.
     *
     * @param pid          The process identifier to set.
     * @param is_increased Whether swappiness should be increased or default.
     * @return Returns true if the underlying system supports this feature, else false.
     */
    public static boolean setSwappiness(int pid, boolean is_increased) {
        return (boolean) XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.setSwappiness,
                pid,
                is_increased
        );
    }

    /**
     * Sets the scheduling group for a process and all child threads
     *
     * @param pid   The identifier of the process to change.
     * @param group The target group for this process from THREAD_GROUP_*.
     * @throws IllegalArgumentException Throws IllegalArgumentException if
     *                                  <var>tid</var> does not exist.
     * @throws SecurityException        Throws SecurityException if your process does
     *                                  not have permission to modify the given thread, or to use the given
     *                                  priority.
     *                                  <p>
     *                                  group == THREAD_GROUP_DEFAULT means to move all non-background priority
     *                                  threads to the foreground scheduling group, but to leave background
     *                                  priority threads alone.  group == THREAD_GROUP_BACKGROUND moves all
     *                                  threads, regardless of priority, to the background scheduling group.
     *                                  group == THREAD_GROUP_FOREGROUND is not allowed.
     *                                  <p>
     *                                  Always sets cpusets.
     */
    public static void setProcessGroup(int pid, int group) {
        XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.setProcessGroup,
                pid,
                group
        );
    }
}
