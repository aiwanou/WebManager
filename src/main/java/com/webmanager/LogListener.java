package com.webmanager;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogListener implements Listener {

    private final List<String> logBuffer;
    private final int maxBufferSize;

    public LogListener(int maxBufferSize) {
        this.logBuffer = new CopyOnWriteArrayList<>();
        this.maxBufferSize = maxBufferSize;
    }

    public LogListener() {
        this(1000); // 默认最大缓存1000条日志
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        String logMessage = "[命令] " + event.getSender().getName() + ": " + event.getCommand();
        addLog(logMessage);
    }

    // 添加日志到缓冲区
    public void addLog(String message) {
        synchronized (logBuffer) {
            logBuffer.add(message);
            if (logBuffer.size() > maxBufferSize) {
                logBuffer.remove(0);
            }
        }
    }

    // 获取所有日志
    public List<String> getLogs() {
        return new ArrayList<>(logBuffer);
    }

    // 获取最近的n条日志
    public List<String> getRecentLogs(int count) {
        synchronized (logBuffer) {
            int startIndex = Math.max(0, logBuffer.size() - count);
            return new ArrayList<>(logBuffer.subList(startIndex, logBuffer.size()));
        }
    }

    // 清空日志缓冲区
    public void clearLogs() {
        synchronized (logBuffer) {
            logBuffer.clear();
        }
    }

    // 注册监听器
    public void register(WebManager plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // 添加初始日志
        addLog("[系统] LogListener 已启动");
    }

    // 注销监听器
    public void unregister() {
        // Bukkit事件监听器会在插件禁用时自动注销
    }
}
