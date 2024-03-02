package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.annotation.AndroidObjectField;
import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.ApplicationIdentity;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.utils.log.ILogger;
import com.venus.backgroundopt.utils.reference.ObjectReference;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#ProcessList}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class ProcessList implements ILogger {
    //不可能的adj取值
    public static final int IMPOSSIBLE_ADJ = Integer.MIN_VALUE;

    // Uninitialized value for any major or minor adj fields
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "INVALID_ADJ")
    public static final int INVALID_ADJ = -10000;

    // Adjustment used in certain places where we don't know it yet.
    // (Generally this is something that is going to be cached, but we
    // don't know the exact value in the cached range to assign yet.)
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "UNKNOWN_ADJ")
    public static final int UNKNOWN_ADJ = 1001;

    // This is a process only hosting activities that are not visible,
    // so it can be killed without any disruption.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "CACHED_APP_MAX_ADJ")
    public static final int CACHED_APP_MAX_ADJ = 999;
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "CACHED_APP_MIN_ADJ")
    public static final int CACHED_APP_MIN_ADJ = 900;

    // This is the oom_adj level that we allow to die first. This cannot be equal to
    // CACHED_APP_MAX_ADJ unless processes are actively being assigned an oom_score_adj of
    // CACHED_APP_MAX_ADJ.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "CACHED_APP_LMK_FIRST_ADJ")
    public static final int CACHED_APP_LMK_FIRST_ADJ = 950;

    // Number of levels we have available for different service connection group importance
    // levels.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "CACHED_APP_IMPORTANCE_LEVELS")
    static final int CACHED_APP_IMPORTANCE_LEVELS = 5;

    // The B list of SERVICE_ADJ -- these are the old and decrepit
    // services that aren't as shiny and interesting as the ones in the A list.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SERVICE_B_ADJ")
    public static final int SERVICE_B_ADJ = 800;

    // This is the process of the previous application that the user was in.
    // This process is kept above other things, because it is very common to
    // switch back to the previous app.  This is important both for recent
    // task switch (toggling between the two top recent apps) as well as normal
    // UI flow such as clicking on a URI in the e-mail app to view in the browser,
    // and then pressing back to return to e-mail.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PREVIOUS_APP_ADJ")
    public static final int PREVIOUS_APP_ADJ = 700;

    // This is a process holding the home application -- we want to try
    // avoiding killing it, even if it would normally be in the background,
    // because the user interacts with it so much.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "HOME_APP_ADJ")
    public static final int HOME_APP_ADJ = 600;

    // This is a process holding an application service -- killing it will not
    // have much of an impact as far as the user is concerned.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SERVICE_ADJ")
    public static final int SERVICE_ADJ = 500;

    // This is a process with a heavy-weight application.  It is in the
    // background, but we want to try to avoid killing it.  Value set in
    // system/rootdir/init.rc on startup.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "HEAVY_WEIGHT_APP_ADJ")
    public static final int HEAVY_WEIGHT_APP_ADJ = 400;

    // This is a process currently hosting a backup operation.  Killing it
    // is not entirely fatal but is generally a bad idea.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "BACKUP_APP_ADJ")
    public static final int BACKUP_APP_ADJ = 300;

    // This is a process bound by the system (or other app) that's more important than services but
    // not so perceptible that it affects the user immediately if killed.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PERCEPTIBLE_LOW_APP_ADJ")
    public static final int PERCEPTIBLE_LOW_APP_ADJ = 250;

    // This is a process hosting services that are not perceptible to the user but the
    // client (system) binding to it requested to treat it as if it is perceptible and avoid killing
    // it if possible.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PERCEPTIBLE_MEDIUM_APP_ADJ")
    public static final int PERCEPTIBLE_MEDIUM_APP_ADJ = 225;

    // This is a process only hosting components that are perceptible to the
    // user, and we really want to avoid killing them, but they are not
    // immediately visible. An example is background music playback.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PERCEPTIBLE_APP_ADJ")
    public static final int PERCEPTIBLE_APP_ADJ = 200;

    // This is a process only hosting activities that are visible to the
    // user, so we'd prefer they don't disappear.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "VISIBLE_APP_ADJ")
    public static final int VISIBLE_APP_ADJ = 100;
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "VISIBLE_APP_LAYER_MAX")
    static final int VISIBLE_APP_LAYER_MAX = PERCEPTIBLE_APP_ADJ - VISIBLE_APP_ADJ - 1;

    // This is a process that was recently TOP and moved to FGS. Continue to treat it almost
    // like a foreground app for a while.
    // @see TOP_TO_FGS_GRACE_PERIOD
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ")
    public static final int PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ = 50;

    // This is the process running the current foreground app.  We'd really
    // rather not kill it!
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "FOREGROUND_APP_ADJ")
    public static final int FOREGROUND_APP_ADJ = 0;

    // This is a process that the system or a persistent process has bound to,
    // and indicated it is important.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PERSISTENT_SERVICE_ADJ")
    public static final int PERSISTENT_SERVICE_ADJ = -700;

    // This is a system persistent process, such as telephony.  Definitely
    // don't want to kill it, but doing so is not completely fatal.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "PERSISTENT_PROC_ADJ")
    public static final int PERSISTENT_PROC_ADJ = -800;

    // The system process runs at the default adjustment.
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SYSTEM_ADJ")
    public static final int SYSTEM_ADJ = -900;

    // Special code for native processes that are not being managed by the system (so
    // don't have an oom adj assigned by the system).
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "NATIVE_ADJ")
    public static final int NATIVE_ADJ = -1000;

    // Activity manager's version of Process.THREAD_GROUP_BACKGROUND
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SCHED_GROUP_BACKGROUND")
    public static final int SCHED_GROUP_BACKGROUND = 0;
    // Activity manager's version of Process.THREAD_GROUP_RESTRICTED
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SCHED_GROUP_RESTRICTED")
    public static final int SCHED_GROUP_RESTRICTED = 1;
    // Activity manager's version of Process.THREAD_GROUP_DEFAULT
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SCHED_GROUP_DEFAULT")
    public static final int SCHED_GROUP_DEFAULT = 2;
    // Activity manager's version of Process.THREAD_GROUP_TOP_APP
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SCHED_GROUP_TOP_APP")
    public static final int SCHED_GROUP_TOP_APP = 3;
    // Activity manager's version of Process.THREAD_GROUP_TOP_APP
    // Disambiguate between actual top app and processes bound to the top app
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = "SCHED_GROUP_TOP_APP_BOUND")
    public static final int SCHED_GROUP_TOP_APP_BOUND = 4;

    @AndroidObject(classPath = ClassConstants.ProcessList)
    private final Object processList;
    // 系统进程列表
    @AndroidObjectField(objectClassPath = ClassConstants.ProcessList, fieldName = FieldConstants.mLruProcesses)
    private final List<?> processRecordList;

    private ActivityManagerService activityManagerService;

    public ProcessList(@AndroidObject Object processList, ActivityManagerService activityManagerService) {
        this.processList = processList;
        this.activityManagerService = activityManagerService;

        this.processRecordList = (List<?>) XposedHelpers.getObjectField(processList, FieldConstants.mLruProcesses);
    }

    @AndroidObjectField
    private List<?> getProcList() {
        return new ArrayList<>(processRecordList);
    }

    private List<?> getCopyOnWriteProcList() {
        return new CopyOnWriteArrayList<>(processRecordList);
    }

    public Map<ApplicationIdentity, List<ProcessRecordKt>> getProcessMap() {
        Map<ApplicationIdentity, List<ProcessRecordKt>> processMap = new HashMap<>();
        List<?> procList = getProcList();
        try {
            ProcessRecordKt processRecord;
            ApplicationIdentity applicationIdentity;
            List<ProcessRecordKt> list;
            for (Object proc : procList) {
                processRecord = new ProcessRecordKt(activityManagerService, proc);
                applicationIdentity = new ApplicationIdentity(processRecord.getUserId(), processRecord.getPackageName());
                list = processMap.computeIfAbsent(applicationIdentity, k -> new ArrayList<>());
                list.add(processRecord);
            }
        } catch (Exception e) {
            getLogger().error("进程列表获取失败", e);
        }

        return processMap;
    }

    public List<ProcessRecordKt> getProcessList(AppInfo appInfo) {
        List<?> procList = getProcList();

        return procList.parallelStream()
                .filter(process -> Objects.equals(ProcessRecordKt.getPkgName(process), appInfo.getPackageName()))
                .map(process -> new ProcessRecordKt(activityManagerService, process))
                .collect(Collectors.toList());
    }

    /**
     * 根据提供的app信息获取进程
     * app主进程会被放在0号位
     *
     * @param appInfo app信息
     * @return 匹配的进程的列表
     */
    public List<ProcessRecordKt> getProcessRecords(AppInfo appInfo) {
        List<ProcessRecordKt> processRecords = new ArrayList<>();
        List<?> procList = getProcList();

        procList.stream()
                .filter(process ->
                        Objects.equals(appInfo.getUserId(), ProcessRecordKt.getUserId(process))
                                && Objects.equals(appInfo.getPackageName(), ProcessRecordKt.getPkgName(process)))
                .forEach(process -> {
                    ProcessRecordKt processRecord = new ProcessRecordKt(activityManagerService, process);

                    if (ProcessRecordKt.isProcessNameSame(appInfo.getPackageName(), process)) {
                        processRecords.add(0, processRecord);
                    } else {
                        processRecords.add(processRecord);
                    }
                });

        return processRecords;
    }

    /**
     * 获取主进程
     *
     * @param appInfo app信息
     * @return 主进程
     */
    private ProcessRecordKt getMProcessRecord(AppInfo appInfo) {
        return getMProcessRecord(appInfo.getPackageName());
    }

    private ProcessRecordKt getMProcessRecord(String packageName) {
        Object process;
        for (int i = processRecordList.size() - 1; i >= 0; i--) {
            process = processRecordList.get(i);
            if (ProcessRecordKt.isProcessNameSame(packageName, process)) {
                return new ProcessRecordKt(activityManagerService, process);
            }
        }

        return null;
    }

    public ProcessRecordKt getMProcessRecordLocked(String packageName) {
        synchronized (activityManagerService.getmProcLock()) {
            return getMProcessRecord(packageName);
        }
    }

    public ProcessRecordKt getMProcessRecordLockedWhenThrowException(AppInfo appInfo) {
        return getMProcessRecordLockedWhenThrowException(appInfo.getPackageName());
    }

    public ProcessRecordKt getMProcessRecordLockedWhenThrowException(String packageName) {
        try {   // 先不使用锁来获取
            return getMProcessRecord(packageName);
        } catch (ConcurrentModificationException e) {   // 出现异常, 加锁获取
            synchronized (activityManagerService.getmProcLock()) {
                return getMProcessRecord(packageName);
            }
        } catch (Exception e) {
            getLogger().error(packageName + "进程列表获取失败", e);
        }
        return null;
    }

    /**
     * 获取指定pid的进程记录器
     *
     * @param pid 进程pid
     */
    public ProcessRecordKt getTargetProcessRecord(int pid) {
        ObjectReference<ProcessRecordKt> processRecord = new ObjectReference<>();
        List<?> procList = getProcList();

        procList.stream()
                .filter(process -> Objects.equals(ProcessRecordKt.getPid(process), pid))
                .findAny()
                .ifPresent(process -> processRecord.set(new ProcessRecordKt(activityManagerService, processRecord)));

        return processRecord.get();
    }
}
