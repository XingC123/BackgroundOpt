package com.venus.backgroundopt.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class FileUtils {
    public static void writeFile(String content, Path path) {
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("文件[ " + path + " ]写入失败");
        }
    }

    public static void appendFile(Path path, String content) {
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("文件 [ " + path + " ]内容追加失败");
        }
    }

    public static List<String> readFile(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
