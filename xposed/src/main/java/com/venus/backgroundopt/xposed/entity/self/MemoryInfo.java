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
                    
package com.venus.backgroundopt.xposed.entity.self;

import com.venus.backgroundopt.common.util.FileUtils;
import com.venus.backgroundopt.common.util.log.ILogger;
import com.venus.backgroundopt.common.util.reference.ObjectReference;

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