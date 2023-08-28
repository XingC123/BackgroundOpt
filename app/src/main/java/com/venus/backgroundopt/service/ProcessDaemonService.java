package com.venus.backgroundopt.service;

import com.venus.backgroundopt.entity.MemoryInfo;
import com.venus.backgroundopt.utils.FileUtils;
import com.venus.backgroundopt.utils.log.ILogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * process-daemon-service 服务
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class ProcessDaemonService implements ILogger {
    private boolean enablePds = true;

    public ProcessDaemonService() {
    }

    /**
     * 初始化process-daemon-service
     */
    public void initPds() throws IOException {
        // 初始化内存信息
        getLogger().info("初始化内存信息");
        memoryInfo.init();

        // 初始化机器的cgroup地址
        initSystemMemcgPath();

        if (!enablePds) {
            return;
        }

        // 初始化分组
        initGroup();
    }

    /* *************************************************************************
     *                                                                         *
     * 获取进程/内存信息的方法                                                     *
     *                                                                         *
     **************************************************************************/
    MemoryInfo memoryInfo = new MemoryInfo();   // 内存信息

    private Integer getTotalMemoryKB() {
        return memoryInfo.getInfo("MemTotal");
    }

    /* *************************************************************************
     *                                                                         *
     * process_daemon_service属性                                               *
     *                                                                         *
     **************************************************************************/
    // process_daemon_service分组的大地址
    private String systemMemcgPath = null;

    // pds active分组地址
    private String pdsActiveGroupPath = null;

    // pds idle分组地址
    private String pdsIdleGroupPath = null;

    enum PdsGroupEnum {
        Active("pds_active"),
        Idle("pds_idle");

        final String pdsGroupName;

        PdsGroupEnum(String pdsGroupName) {
            this.pdsGroupName = pdsGroupName;
        }

        public String getPdsGroupName() {
            return pdsGroupName;
        }
    }

    // 系统默认桌面的包名
    private String launchPackageName;

    /* *************************************************************************
     *                                                                         *
     * process_daemon_service初始化                                             *
     *                                                                         *
     **************************************************************************/
    private void initSystemMemcgPath() {
        getLogger().info("初始化机器的cgroup地址");

        File memcgPath;
        if ((memcgPath = new File("/sys/fs/cgroup/memory")).isDirectory()) {
            systemMemcgPath = memcgPath.getAbsolutePath();
        } else if ((memcgPath = new File("/dev/memcg")).isDirectory()) {
            systemMemcgPath = memcgPath.getAbsolutePath();
        } else {
            // 不支持内存组
            getLogger().info("不支持cgroup, 程序即将退出");
            enablePds = false;
        }

        if (enablePds)
            getLogger().info("初始化机器的cgroup地址完成");
    }

    private void initGroup() throws IOException {
        getLogger().info("初始化PDS分组...");
        initActiveGroup();
        initIdleGroup();
        getLogger().info("初始化PDS分组完成");
    }

    private void initGroupImpl(PdsGroupEnum pdsGroup, int swappiness) throws IOException {
        Path group = Paths.get(systemMemcgPath, pdsGroup.getPdsGroupName());
        String groupPathStr = group.toString();

        if (!Files.isDirectory(group)) {
            Files.createDirectory(group);
        }

        if (pdsGroup == PdsGroupEnum.Active) {
            pdsActiveGroupPath = groupPathStr;
        } else if (pdsGroup == PdsGroupEnum.Idle) {
            pdsIdleGroupPath = groupPathStr;

            String limit = String.format("%sM", getTotalMemoryKB() / 1024 / 10);
            FileUtils.writeFile(limit, Paths.get(groupPathStr, "memory.soft_limit_in_bytes"));
        }

        // 设置swappiness
        FileUtils.writeFile(String.valueOf(swappiness), Paths.get(groupPathStr, "memory.swappiness"));
        /*
            设置oom_control
            1: 禁用
            0: 启用
         */
        FileUtils.writeFile("1", Paths.get(groupPathStr, "memory.oom_control"));

        /*
            启用分层记账，默认禁止。
            内存控制组启用分层记账以后, 子树中的所有内存控制组的内存使用都会被记账到这个内存控制组。
         */
        FileUtils.writeFile("1", Paths.get(groupPathStr, "memory.use_hierarchy"));
    }

    private void initActiveGroup() throws IOException {
        initGroupImpl(PdsGroupEnum.Active, 60);
    }

    private void initIdleGroup() throws IOException {
        initGroupImpl(PdsGroupEnum.Idle, 100);
    }

    /* *************************************************************************
     *                                                                         *
     * 操作process_daemon_service分组                                            *
     *                                                                         *
     **************************************************************************/
    public void movePidToActiveGroup(int pid) {
        movePidToTargetGroup(pid, pdsActiveGroupPath);
    }

    public void movePidToIdleGroup(int pid) {
        movePidToTargetGroup(pid, pdsIdleGroupPath);
    }

    private void movePidToTargetGroup(int pid, String pdsGroupPath) {
        FileUtils.appendFile(Paths.get(pdsGroupPath, "cgroup.procs"), String.valueOf(pid));
    }
}
