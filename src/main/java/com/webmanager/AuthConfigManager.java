package com.webmanager;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class AuthConfigManager {

    private static final String CONFIG_FILE = "auth_config.txt";
    private final File configFile;

    public AuthConfigManager(WebManager plugin) {
        this.configFile = new File(plugin.getDataFolder(), CONFIG_FILE);
        // 确保数据文件夹存在
        plugin.getDataFolder().mkdirs();
    }

    /**
     * 加载授权配置
     * @return 授权IP映射，键为IP地址，值为过期时间
     */
    public Map<String, Long> loadAuthConfig() {
        Map<String, Long> authorizedIps = new HashMap<>();

        if (!configFile.exists()) {
            return authorizedIps;
        }

        try (Scanner scanner = new Scanner(configFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String ip = parts[0].trim();
                    try {
                        long startTime = Long.parseLong(parts[1].trim());
                        long endTime = Long.parseLong(parts[2].trim());
                        authorizedIps.put(ip, endTime);
                    } catch (NumberFormatException e) {
                        // 跳过格式错误的行
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return authorizedIps;
    }

    /**
     * 保存授权配置
     * @param authorizedIps 授权IP映射，键为IP地址，值为过期时间
     */
    public void saveAuthConfig(Map<String, Long> authorizedIps) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
            writer.println("# WebManager 授权配置文件");
            writer.println("# 格式: IP地址, 授权开始时间(毫秒), 授权结束时间(毫秒)");
            writer.println();

            long currentTime = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : authorizedIps.entrySet()) {
                String ip = entry.getKey();
                long endTime = entry.getValue();
                writer.printf("%s, %d, %d%n", ip, currentTime, endTime);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
