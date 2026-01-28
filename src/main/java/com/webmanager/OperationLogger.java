package com.webmanager;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OperationLogger {

    private static final String LOG_FILE = "operation_logs.txt";
    private final File logFile;
    private final SimpleDateFormat dateFormat;

    public OperationLogger(WebManager plugin) {
        this.logFile = new File(plugin.getDataFolder(), LOG_FILE);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 确保数据文件夹存在
        plugin.getDataFolder().mkdirs();
    }

    /**
     * 记录操作日志
     * @param ip IP地址
     * @param category 功能分类
     * @param details 详细操作
     */
    public void logOperation(String ip, String category, String details) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("%s, %s, %s, %s", timestamp, ip, category, details);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取最近的操作日志
     * @param limit 日志数量限制
     * @return 操作日志列表
     */
    public List<String> getRecentLogs(int limit) {
        List<String> logs = new ArrayList<>();

        if (!logFile.exists()) {
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 只返回最近的limit条日志
        if (logs.size() > limit) {
            return logs.subList(logs.size() - limit, logs.size());
        }

        return logs;
    }

    /**
     * 清空操作日志
     */
    public void clearLogs() {
        if (logFile.exists()) {
            logFile.delete();
        }
    }
}
