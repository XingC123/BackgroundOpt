package com.venus.backgroundopt.entity;

import com.venus.backgroundopt.utils.log.ILogger;
import com.venus.backgroundopt.utils.FileUtils;
import com.venus.backgroundopt.utils.reference.ObjectReference;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存信息
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class MemoryInfo implements ILogger {
    private final Map<String, Integer> memInfoMap = new HashMap<>();

    public void init() {
        List<String> lines = FileUtils.readFile(Paths.get("/proc/meminfo"));

        if (lines == null || lines.size() == 0) {
            getLogger().error("读取内存信息出错");
            return;
        }

        ObjectReference<Integer> separatorIndex = new ObjectReference<>();
        ObjectReference<String> key = new ObjectReference<>();
        ObjectReference<Integer> value = new ObjectReference<>();
        lines.forEach(line -> {
            separatorIndex.set(line.indexOf(":"));
            key.set(line.substring(0, separatorIndex.get()));
            value.set(parseMemInfo(line, separatorIndex.get()));

            getLogger().info("key.get(): " + value.get());

            memInfoMap.put(key.get(), value.get());
        });
    }

    public Integer getInfo(String memInfoKey) {
        return memInfoMap.get(memInfoKey);
    }

    /**
     * 内存信息转换
     * 从字符串转到Integer
     *
     * @param memInfo 单行内存信息
     * @return 转换后的内存信息的具体数值
     */
    private Integer parseMemInfo(String memInfo) {
        return parseMemInfo(memInfo, memInfo.indexOf(":"));
    }

    private Integer parseMemInfo(String memInfo, int separatorIndex) {
        return Integer.parseInt(
                memInfo.substring(separatorIndex + 1, memInfo.lastIndexOf("kB")).trim()
        );
    }
}
