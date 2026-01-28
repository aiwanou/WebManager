package com.webmanager;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.io.File;

public class ResourceMonitor {

    private OperatingSystemMXBean osBean;
    private long lastCpuTime;
    private long lastUpTime;

    public ResourceMonitor() {
        osBean = ManagementFactory.getOperatingSystemMXBean();
        lastCpuTime = getProcessCpuTime();
        lastUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
    }

    // 获取进程CPU使用率
    public double getCpuUsage() {
        long currentCpuTime = getProcessCpuTime();
        long currentUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
        
        long cpuTimeDiff = currentCpuTime - lastCpuTime;
        long upTimeDiff = currentUpTime - lastUpTime;
        
        double cpuUsage = 0.0;
        if (upTimeDiff > 0) {
            cpuUsage = (double) cpuTimeDiff / (upTimeDiff * 10000.0);
            cpuUsage = Math.min(100.0, cpuUsage);
        }
        
        lastCpuTime = currentCpuTime;
        lastUpTime = currentUpTime;
        
        return Math.round(cpuUsage * 10) / 10.0;
    }

    // 获取系统CPU使用率（如果支持）
    public double getSystemCpuUsage() {
        try {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            double systemCpuLoad = sunOsBean.getSystemCpuLoad() * 100.0;
            return Math.round(systemCpuLoad * 10) / 10.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // 获取JVM内存使用率
    public double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usage = (double) usedMemory / totalMemory * 100.0;
        return Math.round(usage * 10) / 10.0;
    }

    // 获取系统内存使用率（如果支持）
    public double getSystemMemoryUsage() {
        try {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            long totalPhysicalMemory = sunOsBean.getTotalPhysicalMemorySize();
            long freePhysicalMemory = sunOsBean.getFreePhysicalMemorySize();
            long usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;
            double memoryUsage = (double) usedPhysicalMemory / totalPhysicalMemory * 100.0;
            return Math.round(memoryUsage * 10) / 10.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // 获取当前工作目录所在分区的磁盘使用率
    public double getDiskUsage() {
        File root = new File(System.getProperty("user.dir"));
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        
        double usage = (double) usedSpace / totalSpace * 100.0;
        return Math.round(usage * 10) / 10.0;
    }

    // 获取TPS (Ticks Per Second) - 暂时返回固定值，实际项目中可以使用更复杂的实现
    public double getTPS() {
        // 注意：在实际项目中，应该使用更准确的TPS计算方法
        // 这里为了简单起见，暂时返回一个估计值
        return 20.0;
    }

    // 获取实体数量
    public int getEntityCount() {
        try {
            final java.util.concurrent.atomic.AtomicInteger entityCount = new java.util.concurrent.atomic.AtomicInteger(0);
            org.bukkit.Bukkit.getScheduler().callSyncMethod(org.bukkit.Bukkit.getPluginManager().getPlugins()[0], new java.util.concurrent.Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (org.bukkit.World world : org.bukkit.Bukkit.getServer().getWorlds()) {
                        entityCount.addAndGet(world.getEntities().size());
                    }
                    return null;
                }
            }).get();
            return entityCount.get();
        } catch (Exception e) {
            return 0;
        }
    }

    // 获取在线玩家数量
    public int getOnlinePlayerCount() {
        try {
            final java.util.concurrent.atomic.AtomicInteger playerCount = new java.util.concurrent.atomic.AtomicInteger(0);
            org.bukkit.Bukkit.getScheduler().callSyncMethod(org.bukkit.Bukkit.getPluginManager().getPlugins()[0], new java.util.concurrent.Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    playerCount.set(org.bukkit.Bukkit.getServer().getOnlinePlayers().size());
                    return null;
                }
            }).get();
            return playerCount.get();
        } catch (Exception e) {
            return 0;
        }
    }

    // 获取最大玩家数量
    public int getMaxPlayerCount() {
        try {
            final java.util.concurrent.atomic.AtomicInteger maxPlayers = new java.util.concurrent.atomic.AtomicInteger(0);
            org.bukkit.Bukkit.getScheduler().callSyncMethod(org.bukkit.Bukkit.getPluginManager().getPlugins()[0], new java.util.concurrent.Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    maxPlayers.set(org.bukkit.Bukkit.getServer().getMaxPlayers());
                    return null;
                }
            }).get();
            return maxPlayers.get();
        } catch (Exception e) {
            return 0;
        }
    }

    // 获取加载的区块数量
    public int getLoadedChunksCount() {
        try {
            final java.util.concurrent.atomic.AtomicInteger chunksCount = new java.util.concurrent.atomic.AtomicInteger(0);
            org.bukkit.Bukkit.getScheduler().callSyncMethod(org.bukkit.Bukkit.getPluginManager().getPlugins()[0], new java.util.concurrent.Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for (org.bukkit.World world : org.bukkit.Bukkit.getServer().getWorlds()) {
                        chunksCount.addAndGet(world.getLoadedChunks().length);
                    }
                    return null;
                }
            }).get();
            return chunksCount.get();
        } catch (Exception e) {
            return 0;
        }
    }

    private long getProcessCpuTime() {
        try {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            return sunOsBean.getProcessCpuTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
