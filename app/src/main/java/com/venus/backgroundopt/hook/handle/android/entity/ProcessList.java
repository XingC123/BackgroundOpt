package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.entity.AppInfo;
import com.venus.backgroundopt.entity.ApplicationIdentity;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.utils.log.ILogger;
import com.venus.backgroundopt.utils.reference.ObjectReference;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link com.venus.backgroundopt.hook.constants.ClassConstants#ProcessList}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class ProcessList implements ILogger {
    //不可能的adj取值
    public static final int IMPOSSIBLE_ADJ = Integer.MIN_VALUE;

    // Uninitialized value for any major or minor adj fields
    public static final int INVALID_ADJ = -10000;

    // Adjustment used in certain places where we don't know it yet.
    // (Generally this is something that is going to be cached, but we
    // don't know the exact value in the cached range to assign yet.)
    public static final int UNKNOWN_ADJ = 1001;

    // This is a process only hosting activities that are not visible,
    // so it can be killed without any disruption.
    public static final int CACHED_APP_MAX_ADJ = 999;
    public static final int CACHED_APP_MIN_ADJ = 900;

    // This is the oom_adj level that we allow to die first. This cannot be equal to
    // CACHED_APP_MAX_ADJ unless processes are actively being assigned an oom_score_adj of
    // CACHED_APP_MAX_ADJ.
    public static final int CACHED_APP_LMK_FIRST_ADJ = 950;

    // Number of levels we have available for different service connection group importance
    // levels.
    static final int CACHED_APP_IMPORTANCE_LEVELS = 5;

    // The B list of SERVICE_ADJ -- these are the old and decrepit
    // services that aren't as shiny and interesting as the ones in the A list.
    public static final int SERVICE_B_ADJ = 800;

    // This is the process of the previous application that the user was in.
    // This process is kept above other things, because it is very common to
    // switch back to the previous app.  This is important both for recent
    // task switch (toggling between the two top recent apps) as well as normal
    // UI flow such as clicking on a URI in the e-mail app to view in the browser,
    // and then pressing back to return to e-mail.
    public static final int PREVIOUS_APP_ADJ = 700;

    // This is a process holding the home application -- we want to try
    // avoiding killing it, even if it would normally be in the background,
    // because the user interacts with it so much.
    public static final int HOME_APP_ADJ = 600;

    // This is a process holding an application service -- killing it will not
    // have much of an impact as far as the user is concerned.
    public static final int SERVICE_ADJ = 500;

    // This is a process with a heavy-weight application.  It is in the
    // background, but we want to try to avoid killing it.  Value set in
    // system/rootdir/init.rc on startup.
    public static final int HEAVY_WEIGHT_APP_ADJ = 400;

    // This is a process currently hosting a backup operation.  Killing it
    // is not entirely fatal but is generally a bad idea.
    public static final int BACKUP_APP_ADJ = 300;

    // This is a process bound by the system (or other app) that's more important than services but
    // not so perceptible that it affects the user immediately if killed.
    public static final int PERCEPTIBLE_LOW_APP_ADJ = 250;

    // This is a process hosting services that are not perceptible to the user but the
    // client (system) binding to it requested to treat it as if it is perceptible and avoid killing
    // it if possible.
    public static final int PERCEPTIBLE_MEDIUM_APP_ADJ = 225;

    // This is a process only hosting components that are perceptible to the
    // user, and we really want to avoid killing them, but they are not
    // immediately visible. An example is background music playback.
    public static final int PERCEPTIBLE_APP_ADJ = 200;

    // This is a process only hosting activities that are visible to the
    // user, so we'd prefer they don't disappear.
    public static final int VISIBLE_APP_ADJ = 100;
    static final int VISIBLE_APP_LAYER_MAX = PERCEPTIBLE_APP_ADJ - VISIBLE_APP_ADJ - 1;

    // This is a process that was recently TOP and moved to FGS. Continue to treat it almost
    // like a foreground app for a while.
    // @see TOP_TO_FGS_GRACE_PERIOD
    public static final int PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ = 50;

    // This is the process running the current foreground app.  We'd really
    // rather not kill it!
    public static final int FOREGROUND_APP_ADJ = 0;

    // This is a process that the system or a persistent process has bound to,
    // and indicated it is important.
    public static final int PERSISTENT_SERVICE_ADJ = -700;

    // This is a system persistent process, such as telephony.  Definitely
    // don't want to kill it, but doing so is not completely fatal.
    public static final int PERSISTENT_PROC_ADJ = -800;

    // The system process runs at the default adjustment.
    public static final int SYSTEM_ADJ = -900;

    // Special code for native processes that are not being managed by the system (so
    // don't have an oom adj assigned by the system).
    public static final int NATIVE_ADJ = -1000;

    // Activity manager's version of Process.THREAD_GROUP_BACKGROUND
    public static final int SCHED_GROUP_BACKGROUND = 0;
    // Activity manager's version of Process.THREAD_GROUP_RESTRICTED
    public static final int SCHED_GROUP_RESTRICTED = 1;
    // Activity manager's version of Process.THREAD_GROUP_DEFAULT
    public static final int SCHED_GROUP_DEFAULT = 2;
    // Activity manager's version of Process.THREAD_GROUP_TOP_APP
    public static final int SCHED_GROUP_TOP_APP = 3;
    // Activity manager's version of Process.THREAD_GROUP_TOP_APP
    // Disambiguate between actual top app and processes bound to the top app
    public static final int SCHED_GROUP_TOP_APP_BOUND = 4;

    private final Object processList;
    // 系统进程列表
    private final List<?> processRecordList;

    private ActivityManagerService activityManagerService;

    public ProcessList(Object processList, ActivityManagerService activityManagerService) {
        this.processList = processList;
        this.activityManagerService = activityManagerService;

        this.processRecordList = (List<?>) XposedHelpers.getObjectField(processList, FieldConstants.mLruProcesses);
    }

    private List<?> getProcList() {
        return new ArrayList<>(processRecordList);
    }

    private List<?> getCopyOnWriteProcList() {
        return new CopyOnWriteArrayList<>(processRecordList);
    }

    public Map<ApplicationIdentity, List<ProcessRecord>> getProcessMap() {
        Map<ApplicationIdentity, List<ProcessRecord>> processMap = new HashMap<>();
        List<?> procList = getProcList();
        try {
            ProcessRecord processRecord;
            ApplicationIdentity applicationIdentity;
            List<ProcessRecord> list;
            for (Object proc : procList) {
                processRecord = new ProcessRecord(proc);
                applicationIdentity = new ApplicationIdentity(processRecord.getUserId(), processRecord.getPackageName());
                list = processMap.computeIfAbsent(applicationIdentity, k -> new ArrayList<>());
                list.add(processRecord);
            }
        } catch (Exception e) {
            getLogger().error("进程列表获取失败", e);
        }

        return processMap;
    }

    public List<ProcessRecord> getProcessList(AppInfo appInfo) {
        List<?> procList = getProcList();

        return procList.parallelStream()
                .filter(process -> Objects.equals(ProcessRecord.getPkgName(process), appInfo.getPackageName()))
                .map(ProcessRecord::new)
                .collect(Collectors.toList());
    }

    /**
     * 根据提供的app信息获取进程
     * app主进程会被放在0号位
     *
     * @param appInfo app信息
     * @return 匹配的进程的列表
     */
    public List<ProcessRecord> getProcessRecords(AppInfo appInfo) {
        List<ProcessRecord> processRecords = new ArrayList<>();
        List<?> procList = getProcList();

        procList.stream()
                .filter(process ->
                        Objects.equals(appInfo.getUserId(), ProcessRecord.getUserId(process))
                                && Objects.equals(appInfo.getPackageName(), ProcessRecord.getPkgName(process)))
                .forEach(process -> {
                    ProcessRecord processRecord = new ProcessRecord(process);

                    if (ProcessRecord.isProcessNameSame(appInfo.getPackageName(), process)) {
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
    private ProcessRecord getMProcessRecord(AppInfo appInfo) {
        Optional<ProcessRecord> processRecord = processRecordList.parallelStream()
                .filter(proc -> ProcessRecord.isProcessNameSame(appInfo.getPackageName(), proc))
                .findAny()
                .map(ProcessRecord::new);
        return processRecord.orElse(null);
    }

    public ProcessRecord getMProcessRecordLockedWhenThrowException(AppInfo appInfo) {
        try {   // 先不使用锁来获取
            return getMProcessRecord(appInfo);
        } catch (ConcurrentModificationException e) {   // 出现异常, 加锁获取
            synchronized (activityManagerService.getmProcLock()) {
                return getMProcessRecord(appInfo);
            }
        } catch (Exception e) {
            getLogger().error(appInfo.getPackageName() + "进程列表获取失败", e);
        }
        return null;
    }

    /**
     * 获取指定pid的进程记录器
     *
     * @param pid 进程pid
     */
    public ProcessRecord getTargetProcessRecord(int pid) {
        ObjectReference<ProcessRecord> processRecord = new ObjectReference<>();
        List<?> procList = getProcList();

        procList.stream()
                .filter(process -> Objects.equals(ProcessRecord.getPid(process), pid))
                .findAny()
                .ifPresent(process -> processRecord.set(new ProcessRecord(processRecord)));

        return processRecord.get();
    }
}
