package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/20
 */
public class Process {
    // Keep in sync with SP_* constants of enum type SchedPolicy
    // declared in system/core/include/cutils/sched_policy.h,
    // except THREAD_GROUP_DEFAULT does not correspond to any SP_* value.

    /**
     * Default thread group -
     * has meaning with setProcessGroup() only, cannot be used with setThreadGroup().
     * When used with setProcessGroup(), the group of each thread in the process
     * is conditionally changed based on that thread's current priority, as follows:
     * threads with priority numerically less than THREAD_PRIORITY_BACKGROUND
     * are moved to foreground thread group.  All other threads are left unchanged.
     */
    public static final int THREAD_GROUP_DEFAULT = -1;

    /**
     * Background thread group - All threads in
     * this group are scheduled with a reduced share of the CPU.
     * Value is same as constant SP_BACKGROUND of enum SchedPolicy.
     */
    public static final int THREAD_GROUP_BACKGROUND = 0;

    /**
     * Foreground thread group - All threads in
     * this group are scheduled with a normal share of the CPU.
     * Value is same as constant SP_FOREGROUND of enum SchedPolicy.
     * Not used at this level.
     **/
    private static final int THREAD_GROUP_FOREGROUND = 1;

    /**
     * System thread group.
     **/
    public static final int THREAD_GROUP_SYSTEM = 2;

    /**
     * Application audio thread group.
     **/
    public static final int THREAD_GROUP_AUDIO_APP = 3;

    /**
     * System audio thread group.
     **/
    public static final int THREAD_GROUP_AUDIO_SYS = 4;

    /**
     * Thread group for top foreground app.
     **/
    public static final int THREAD_GROUP_TOP_APP = 5;

    /**
     * Thread group for RT app.
     **/
    public static final int THREAD_GROUP_RT_APP = 6;

    /**
     * Thread group for bound foreground services that should
     * have additional CPU restrictions during screen off
     **/
    public static final int THREAD_GROUP_RESTRICTED = 7;

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

    /**
     * Return the scheduling group of requested process.
     */
    public static int getProcessGroup(int pid) {
        return (int) XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.getProcessGroup,
                pid
        );
    }

    /**
     * Sets the scheduling group and the corresponding cpuset group
     *
     * @param tid   The identifier of the thread to change.
     * @param group The target group for this thread from THREAD_GROUP_*.
     * @throws IllegalArgumentException Throws IllegalArgumentException if
     *                                  <var>tid</var> does not exist.
     * @throws SecurityException        Throws SecurityException if your process does
     *                                  not have permission to modify the given thread, or to use the given
     *                                  priority.
     */
    public static void setThreadGroupAndCpuset(int tid, int group) {
        XposedHelpers.callStaticMethod(
                getProcess(),
                MethodConstants.setThreadGroupAndCpuset,
                tid,
                group
        );
    }
}
