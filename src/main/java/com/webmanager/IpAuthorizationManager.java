package com.webmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IpAuthorizationManager {

    private final Map<String, Long> authorizedIps;
    private final AuthConfigManager authConfigManager;

    public IpAuthorizationManager(WebManager plugin) {
        this.authConfigManager = new AuthConfigManager(plugin);
        this.authorizedIps = authConfigManager.loadAuthConfig();
    }

    /**
     * 添加授权IP
     * @param ip IP地址
     * @param days 授权天数
     */
    public void addAuthorizedIp(String ip, int days) {
        long expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
        authorizedIps.put(ip, expirationTime);
        saveConfig();
    }

    /**
     * 移除授权IP
     * @param ip IP地址
     */
    public void removeAuthorizedIp(String ip) {
        authorizedIps.remove(ip);
        saveConfig();
    }

    /**
     * 检查IP是否已授权
     * @param ip IP地址
     * @return 是否授权
     */
    public boolean isAuthorized(String ip) {
        if (!authorizedIps.containsKey(ip)) {
            return false;
        }

        long expirationTime = authorizedIps.get(ip);
        if (System.currentTimeMillis() > expirationTime) {
            // 授权已过期，移除
            authorizedIps.remove(ip);
            saveConfig();
            return false;
        }

        return true;
    }

    /**
     * 获取所有授权IP
     * @return 授权IP映射
     */
    public Map<String, Long> getAuthorizedIps() {
        return new HashMap<>(authorizedIps);
    }

    /**
     * 清理过期的授权IP
     */
    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        boolean changed = authorizedIps.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        if (changed) {
            saveConfig();
        }
    }

    /**
     * 保存配置到文件
     */
    private void saveConfig() {
        authConfigManager.saveAuthConfig(authorizedIps);
    }
}
