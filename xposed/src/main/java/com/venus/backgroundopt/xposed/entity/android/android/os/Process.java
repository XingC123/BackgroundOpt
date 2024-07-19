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
                    
package com.venus.backgroundopt.xposed.entity.android.android.os;

import com.venus.backgroundopt.xposed.annotation.OriginalObject;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;

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


    public static final int PROC_TERM_MASK = 0xff;
    public static final int PROC_ZERO_TERM = 0;
    public static final int PROC_SPACE_TERM = (int) ' ';
    public static final int PROC_TAB_TERM = (int) '\t';
    public static final int PROC_NEWLINE_TERM = (int) '\n';
    public static final int PROC_COMBINE = 0x100;
    public static final int PROC_PARENS = 0x200;
    public static final int PROC_QUOTES = 0x400;
    public static final int PROC_CHAR = 0x800;
    public static final int PROC_OUT_STRING = 0x1000;
    public static final int PROC_OUT_LONG = 0x2000;
    public static final int PROC_OUT_FLOAT = 0x4000;

    @OriginalObject(classPath = ClassConstants.Process)
    private Object process;

    public static Class<?> getProcess() {
        return android.os.Process.class;
    }

    public Process(@OriginalObject Object process) {
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

    @SuppressWarnings("rawtypes")
    private static final Class[] readProcFileParamClasses = new Class[]{
            String.class, int[].class, String[].class, long[].class, float[].class
    };

    /**
     * Read and parse a {@code proc} file in the given format.
     *
     * <p>The format is a list of integers, where every integer describes a variable in the file. It
     * specifies how the variable is syntactically terminated (e.g. {@link Process#PROC_SPACE_TERM},
     * {@link Process#PROC_TAB_TERM}, {@link Process#PROC_ZERO_TERM}, {@link
     * Process#PROC_NEWLINE_TERM}).
     *
     * <p>If the variable should be parsed and returned to the caller, the termination type should
     * be binary OR'd with the type of output (e.g. {@link Process#PROC_OUT_STRING}, {@link
     * Process#PROC_OUT_LONG}, {@link Process#PROC_OUT_FLOAT}.
     *
     * <p>If the variable is wrapped in quotation marks it should be binary OR'd with {@link
     * Process#PROC_QUOTES}. If the variable is wrapped in parentheses it should be binary OR'd with
     * {@link Process#PROC_PARENS}.
     *
     * <p>If the variable is not formatted as a string and should be cast directly from characters
     * to a long, the {@link Process#PROC_CHAR} integer should be binary OR'd.
     *
     * <p>If the terminating character can be repeated, the {@link Process#PROC_COMBINE} integer
     * should be binary OR'd.
     *
     * @param file       the path of the {@code proc} file to read
     * @param format     the format of the file
     * @param outStrings the parsed {@code String}s from the file
     * @param outLongs   the parsed {@code long}s from the file
     * @param outFloats  the parsed {@code float}s from the file
     */
    public static void readProcFile(String file, int[] format,
                                    String[] outStrings, long[] outLongs, float[] outFloats) {
        XposedHelpers.callStaticMethod(
                Process.getProcess(),
                MethodConstants.readProcFile,
                readProcFileParamClasses,
                file,
                format,
                outStrings,
                outLongs,
                outFloats
        );
    }
}