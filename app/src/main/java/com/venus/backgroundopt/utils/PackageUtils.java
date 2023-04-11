package com.venus.backgroundopt.utils;

public class PackageUtils {

    /**
     * 绝对进程名
     *
     * @param packageName 包名
     * @param processName 进程名
     */
    public static String absoluteProcessName(String packageName, String processName) {
        // 相对进程名
        if (processName.startsWith(".")) {
            // 拼成绝对进程名
            return packageName + ":" + processName.substring(1);
        } else {
            // 是绝对进程直接返回
            return processName;
        }
    }
}
