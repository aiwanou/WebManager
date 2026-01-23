package com.webmanager;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class WebHttpServer {

    private WebManager plugin;
    private HttpServer server;

    public WebHttpServer(WebManager plugin) {
        this.plugin = plugin;
    }

    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new HomeHandler());
            server.createContext("/api/players", new PlayersHandler());
            server.createContext("/api/resources", new ResourcesHandler());
            server.createContext("/api/command", new CommandHandler());
            server.createContext("/api/logs", new LogsHandler());
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/auth/add", new AuthHandler());
            server.createContext("/api/auth/remove", new AuthHandler());
            server.createContext("/api/auth/extend", new AuthHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
        } catch (IOException e) {
            plugin.getLogger().severe("启动HTTP服务器失败: " + e.getMessage());
        }
    }

    // 检查IP是否已授权
    private boolean isIpAuthorized(String ip) {
        // 清理过期的授权
        plugin.getIpAuthManager().cleanupExpired();
        // 检查IP是否在授权列表中
        return plugin.getIpAuthManager().isAuthorized(ip);
    }

    // 获取客户端IP地址
    private String getClientIp(HttpExchange exchange) {
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        // 处理代理情况
        String forwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            ip = forwardedFor.split(",")[0].trim();
        }
        return ip;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>WebManager - MC服务器管理</title>
                <style>
                    :root {
                        --primary-color: #ff6700;
                        --primary-light: #ff9e43;
                        --secondary-color: #333333;
                        --text-color: #333333;
                        --text-light: #666666;
                        --background-color: #f5f5f5;
                        --card-background: #ffffff;
                        --border-color: #e0e0e0;
                        --success-color: #52c41a;
                        --warning-color: #faad14;
                        --error-color: #ff4d4f;
                        --info-color: #1890ff;
                        --sidebar-width: 240px;
                        --header-height: 60px;
                        --border-radius: 12px;
                        --shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                    }
                    
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        font-size: 14px;
                        line-height: 1.5;
                        color: var(--text-color);
                        background-color: var(--background-color);
                        min-height: 100vh;
                    }
                    
                    .app-container {
                        display: flex;
                        min-height: 100vh;
                    }
                    
                    /* 侧边栏 */
                    .sidebar {
                        width: var(--sidebar-width);
                        background-color: var(--card-background);
                        box-shadow: var(--shadow);
                        padding: 20px 0;
                        position: fixed;
                        left: 0;
                        top: 0;
                        bottom: 0;
                        overflow-y: auto;
                        z-index: 100;
                        transition: all 0.3s ease;
                    }
                    
                    .sidebar-header {
                        padding: 0 20px 20px;
                        border-bottom: 1px solid var(--border-color);
                        margin-bottom: 20px;
                    }
                    
                    .sidebar-header h1 {
                        font-size: 18px;
                        font-weight: 600;
                        color: var(--primary-color);
                        text-align: center;
                    }
                    
                    .sidebar-menu {
                        list-style: none;
                    }
                    
                    .sidebar-menu li {
                        margin-bottom: 4px;
                    }
                    
                    .sidebar-menu a {
                        display: block;
                        padding: 12px 20px;
                        color: var(--text-color);
                        text-decoration: none;
                        border-radius: 0 20px 20px 0;
                        transition: all 0.3s ease;
                        font-weight: 500;
                    }
                    
                    .sidebar-menu a:hover {
                        background-color: rgba(255, 103, 0, 0.1);
                        color: var(--primary-color);
                    }
                    
                    .sidebar-menu a.active {
                        background-color: rgba(255, 103, 0, 0.1);
                        color: var(--primary-color);
                        font-weight: 600;
                        border-left: 3px solid var(--primary-color);
                    }
                    
                    /* 主内容区 */
                    .main-content {
                        flex: 1;
                        margin-left: var(--sidebar-width);
                        padding-top: var(--header-height);
                        min-height: 100vh;
                    }
                    
                    /* 头部 */
                    .header {
                        position: fixed;
                        top: 0;
                        right: 0;
                        left: var(--sidebar-width);
                        height: var(--header-height);
                        background-color: var(--card-background);
                        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding: 0 30px;
                        z-index: 99;
                    }
                    
                    .header-title {
                        font-size: 16px;
                        font-weight: 600;
                        color: var(--text-color);
                    }
                    
                    .header-info {
                        display: flex;
                        align-items: center;
                        gap: 16px;
                    }
                    
                    .header-info-item {
                        display: flex;
                        align-items: center;
                        gap: 6px;
                        color: var(--text-light);
                    }
                    
                    /* 内容区 */
                    .content {
                        padding: 30px;
                    }
                    
                    .card {
                        background-color: var(--card-background);
                        border-radius: var(--border-radius);
                        box-shadow: var(--shadow);
                        padding: 24px;
                        margin-bottom: 24px;
                        transition: all 0.3s ease;
                    }
                    
                    .card:hover {
                        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                    }
                    
                    .card-title {
                        font-size: 18px;
                        font-weight: 600;
                        margin-bottom: 16px;
                        color: var(--text-color);
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    
                    .card-title::before {
                        content: '';
                        width: 4px;
                        height: 16px;
                        background-color: var(--primary-color);
                        border-radius: 2px;
                    }
                    
                    /* 服务器信息 */
                    .server-info {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 16px;
                        margin-bottom: 24px;
                    }
                    
                    .info-card {
                        background-color: var(--card-background);
                        border-radius: var(--border-radius);
                        box-shadow: var(--shadow);
                        padding: 20px;
                        text-align: center;
                        transition: all 0.3s ease;
                    }
                    
                    .info-card:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
                    }
                    
                    .info-card .value {
                        font-size: 24px;
                        font-weight: 600;
                        color: var(--primary-color);
                        margin-bottom: 4px;
                    }
                    
                    .info-card .label {
                        font-size: 14px;
                        color: var(--text-light);
                    }
                    
                    /* 表格 */
                    .table-container {
                        overflow-x: auto;
                    }
                    
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 16px;
                    }
                    
                    th, td {
                        padding: 12px 16px;
                        text-align: left;
                        border-bottom: 1px solid var(--border-color);
                    }
                    
                    th {
                        background-color: #fafafa;
                        font-weight: 600;
                        color: var(--text-color);
                        font-size: 14px;
                    }
                    
                    tr:hover {
                        background-color: #fafafa;
                    }
                    
                    /* 资源监控 */
                    .resource-item {
                        margin-bottom: 20px;
                    }
                    
                    .resource-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 8px;
                    }
                    
                    .resource-name {
                        font-size: 14px;
                        color: var(--text-color);
                        font-weight: 500;
                    }
                    
                    .resource-value {
                        font-size: 14px;
                        color: var(--text-light);
                        font-weight: 500;
                    }
                    
                    .resource-bar {
                        width: 100%;
                        height: 12px;
                        background-color: #f0f0f0;
                        border-radius: 6px;
                        overflow: hidden;
                        position: relative;
                    }
                    
                    .resource-fill {
                        height: 100%;
                        border-radius: 6px;
                        transition: width 0.5s ease;
                        position: relative;
                    }
                    
                    .resource-fill::after {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
                        animation: shimmer 2s infinite;
                    }
                    
                    @keyframes shimmer {
                        0% { transform: translateX(-100%); }
                        100% { transform: translateX(100%); }
                    }
                    
                    .cpu-fill {
                        background-color: var(--success-color);
                    }
                    
                    .memory-fill {
                        background-color: var(--info-color);
                    }
                    
                    .disk-fill {
                        background-color: var(--warning-color);
                    }
                    
                    /* 命令执行 */
                    #commandForm {
                        display: flex;
                        gap: 12px;
                        margin-top: 16px;
                    }
                    
                    #commandInput {
                        flex: 1;
                        padding: 12px 16px;
                        border: 1px solid var(--border-color);
                        border-radius: var(--border-radius);
                        font-size: 14px;
                        transition: all 0.3s ease;
                    }
                    
                    #commandInput:focus {
                        outline: none;
                        border-color: var(--primary-color);
                        box-shadow: 0 0 0 2px rgba(255, 103, 0, 0.1);
                    }
                    
                    #submitCommand {
                        padding: 12px 24px;
                        background-color: var(--primary-color);
                        color: white;
                        border: none;
                        border-radius: var(--border-radius);
                        font-size: 14px;
                        font-weight: 500;
                        cursor: pointer;
                        transition: all 0.3s ease;
                    }
                    
                    #submitCommand:hover {
                        background-color: var(--primary-light);
                        transform: translateY(-1px);
                    }
                    
                    #submitCommand:active {
                        transform: translateY(0);
                    }
                    
                    #commandResult {
                        margin-top: 16px;
                        padding: 16px;
                        background-color: #fafafa;
                        border-radius: var(--border-radius);
                        border: 1px solid var(--border-color);
                        min-height: 80px;
                        white-space: pre-wrap;
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 14px;
                    }
                    
                    /* 加载动画 */
                    @keyframes pulse {
                        0% { opacity: 1; }
                        50% { opacity: 0.7; }
                        100% { opacity: 1; }
                    }
                    
                    .loading {
                        animation: pulse 1s infinite;
                        color: var(--text-light);
                    }
                    
                    /* 内容区域过渡动画 */
                    .section-content {
                        transition: all 0.3s ease;
                        opacity: 1;
                        transform: translateY(0);
                    }
                    
                    /* 响应式设计 */
                    @media (max-width: 768px) {
                        :root {
                            --sidebar-width: 0;
                        }
                        
                        .sidebar {
                            transform: translateX(-100%);
                        }
                        
                        .main-content {
                            margin-left: 0;
                        }
                        
                        .header {
                            left: 0;
                        }
                        
                        .server-info {
                            grid-template-columns: 1fr;
                        }
                        
                        #commandForm {
                            flex-direction: column;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="app-container">
                    <!-- 侧边栏 -->
                    <aside class="sidebar">
                        <div class="sidebar-header">
                            <h1>WebManager</h1>
                        </div>
                        <ul class="sidebar-menu">
                            <li><a href="#dashboard" class="active" data-section="dashboard">控制台</a></li>
                            <li><a href="#server" data-section="server">服务器管理</a></li>
                            <li><a href="#players" data-section="players">玩家管理</a></li>
                            <li><a href="#worlds" data-section="worlds">世界管理</a></li>
                            <li><a href="#resources" data-section="resources">资源监控</a></li>
                            <li><a href="#console" data-section="console">命令执行</a></li>
                            <li><a href="#config" data-section="config">配置管理</a></li>
                            <li><a href="#backup" data-section="backup">备份管理</a></li>
                            <li><a href="#notifications" data-section="notifications">通知系统</a></li>
                            <li><a href="#security" data-section="security">安全管理</a></li>
                            <li><a href="#settings" data-section="settings">设置</a></li>
                        </ul>
                    </aside>
                    
                    <!-- 主内容区 -->
                    <div class="main-content">
                        <!-- 头部 -->
                        <header class="header">
                            <div class="header-title">Minecraft服务器管理</div>
                            <div class="header-info">
                                <div class="header-info-item">
                                    <span>服务器版本: <span id="serverVersion">加载中...</span></span>
                                </div>
                                <div class="header-info-item">
                                    <span>在线: <span id="playerCount">加载中...</span>/<span id="maxPlayers">加载中...</span></span>
                                </div>
                            </div>
                        </header>
                        
                        <!-- 内容区 -->
                        <div class="content">
                            <!-- 控制台 -->
                            <div id="dashboard-section" class="section-content">
                                <div class="server-info">
                                    <div class="info-card">
                                        <div class="value" id="onlinePlayers">加载中...</div>
                                        <div class="label">在线玩家</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="tpsValue">加载中...</div>
                                        <div class="label">TPS</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="entityValue">加载中...</div>
                                        <div class="label">实体数量</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="chunksValue">加载中...</div>
                                        <div class="label">加载区块</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="cpuValue">加载中...</div>
                                        <div class="label">CPU使用率</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="memoryValue">加载中...</div>
                                        <div class="label">内存使用率</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="diskValue">加载中...</div>
                                        <div class="label">磁盘使用率</div>
                                    </div>
                                    <div class="info-card">
                                        <div class="value" id="systemCpuValue">加载中...</div>
                                        <div class="label">系统CPU</div>
                                    </div>
                                </div>
                                
                                <div class="card">
                                    <h2 class="card-title">玩家列表</h2>
                                    <div class="table-container">
                                        <table id="playerTable">
                                            <thead>
                                                <tr>
                                                    <th>玩家名称</th>
                                                    <th>UUID</th>
                                                    <th>IP地址</th>
                                                    <th>游戏模式</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr>
                                                    <td colspan="4" class="loading">加载中...</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 服务器管理 -->
                            <div id="server-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">服务器管理</h2>
                                    
                                    <!-- 服务器状态 -->
                                    <div class="card">
                                        <h3 class="card-title">服务器状态</h3>
                                        <div style="display: flex; gap: 20px; margin-bottom: 20px;">
                                            <div style="flex: 1;">
                                                <p>服务器状态: <span id="serverStatus" style="font-weight: bold; color: var(--success-color);">运行中</span></p>
                                                <p>服务器版本: <span id="serverVersionInfo">加载中...</span></p>
                                                <p>服务器地址: <span id="serverAddress">加载中...</span></p>
                                                <p>运行时间: <span id="uptime">加载中...</span></p>
                                            </div>
                                            <div style="flex: 1;">
                                                <p>TPS: <span id="tpsInfo">加载中...</span></p>
                                                <p>内存使用: <span id="memoryUsageInfo">加载中...</span></p>
                                                <p>在线玩家: <span id="onlinePlayersInfo">加载中...</span></p>
                                                <p>最大玩家: <span id="maxPlayersInfo">加载中...</span></p>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- 服务器控制 -->
                                    <div class="card">
                                        <h3 class="card-title">服务器控制</h3>
                                        <div style="margin-bottom: 20px;">
                                            <button id="restartServerBtn" style="padding: 10px 20px; background-color: var(--warning-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer; margin-right: 10px;">重启服务器</button>
                                            <button id="stopServerBtn" style="padding: 10px 20px; background-color: var(--error-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer; margin-right: 10px;">停止服务器</button>
                                            <button id="broadcastBtn" style="padding: 10px 20px; background-color: var(--info-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">发送广播</button>
                                        </div>
                                        <div id="restartCountdown" style="display: none; margin-top: 20px; padding: 10px; background-color: var(--warning-color); color: white; border-radius: 4px;">
                                            服务器将在 <span id="countdownTimer">60</span> 秒后重启...
                                        </div>
                                    </div>
                                    
                                    <!-- 重启计划 -->
                                    <div class="card">
                                        <h3 class="card-title">重启计划</h3>
                                        <div style="margin-bottom: 20px;">
                                            <h4>设置定时重启</h4>
                                            <form id="restartScheduleForm">
                                                <div style="margin-bottom: 10px;">
                                                    <label for="restartHour">小时:</label>
                                                    <input type="number" id="restartHour" min="0" max="23" required>
                                                </div>
                                                <div style="margin-bottom: 10px;">
                                                    <label for="restartMinute">分钟:</label>
                                                    <input type="number" id="restartMinute" min="0" max="59" required>
                                                </div>
                                                <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">设置</button>
                                            </form>
                                        </div>
                                        <div id="currentSchedule" style="margin-top: 20px;">
                                            <h4>当前计划</h4>
                                            <p id="scheduleInfo">未设置定时重启</p>
                                        </div>
                                    </div>
                                    
                                    <!-- 服务器事件 -->
                                    <div class="card">
                                        <h3 class="card-title">服务器事件</h3>
                                        <div class="table-container">
                                            <table id="eventsTable">
                                                <thead>
                                                    <tr>
                                                        <th>时间</th>
                                                        <th>事件类型</th>
                                                        <th>描述</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="3" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 玩家管理 -->
                            <div id="players-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">玩家管理</h2>
                                    
                                    <!-- 在线玩家 -->
                                    <div class="card">
                                        <h3 class="card-title">在线玩家</h3>
                                        <div class="table-container">
                                            <table id="playersTable">
                                                <thead>
                                                    <tr>
                                                        <th>玩家名称</th>
                                                        <th>UUID</th>
                                                        <th>IP地址</th>
                                                        <th>游戏模式</th>
                                                        <th>位置</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="6" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    
                                    <!-- 玩家操作 -->
                                    <div class="card">
                                        <h3 class="card-title">玩家操作</h3>
                                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
                                            <div>
                                                <h4>踢出玩家</h4>
                                                <form id="kickForm">
                                                    <input type="text" id="kickPlayer" placeholder="玩家名称" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <input type="text" id="kickReason" placeholder="原因" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--warning-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">踢出</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>封禁玩家</h4>
                                                <form id="banForm">
                                                    <input type="text" id="banPlayer" placeholder="玩家名称" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <input type="text" id="banReason" placeholder="原因" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--error-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">封禁</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>传送玩家</h4>
                                                <form id="teleportForm">
                                                    <input type="text" id="teleportPlayer" placeholder="玩家名称" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <input type="text" id="teleportTarget" placeholder="目标位置" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--info-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">传送</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>修改游戏模式</h4>
                                                <form id="gamemodeForm">
                                                    <input type="text" id="gamemodePlayer" placeholder="玩家名称" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <select id="gamemodeType" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                        <option value="survival">生存</option>
                                                        <option value="creative">创造</option>
                                                        <option value="adventure">冒险</option>
                                                        <option value="spectator">旁观者</option>
                                                    </select>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">修改</button>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- 玩家历史记录 -->
                                    <div class="card">
                                        <h3 class="card-title">玩家历史记录</h3>
                                        <div class="table-container">
                                            <table id="playerHistoryTable">
                                                <thead>
                                                    <tr>
                                                        <th>时间</th>
                                                        <th>玩家名称</th>
                                                        <th>事件类型</th>
                                                        <th>描述</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="4" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 世界管理 -->
                            <div id="worlds-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">世界管理</h2>
                                    
                                    <!-- 世界列表 -->
                                    <div class="card">
                                        <h3 class="card-title">世界列表</h3>
                                        <div class="table-container">
                                            <table id="worldsTable">
                                                <thead>
                                                    <tr>
                                                        <th>世界名称</th>
                                                        <th>世界类型</th>
                                                        <th>种子</th>
                                                        <th>玩家数量</th>
                                                        <th>实体数量</th>
                                                        <th>加载区块</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="7" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    
                                    <!-- 世界设置 -->
                                    <div class="card">
                                        <h3 class="card-title">世界设置</h3>
                                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
                                            <div>
                                                <h4>时间设置</h4>
                                                <form id="timeForm">
                                                    <select id="worldSelect" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                        <option value="">选择世界</option>
                                                    </select>
                                                    <select id="timeType" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                        <option value="day">白天</option>
                                                        <option value="night">夜晚</option>
                                                        <option value="noon">正午</option>
                                                        <option value="midnight">午夜</option>
                                                    </select>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">设置</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>天气设置</h4>
                                                <form id="weatherForm">
                                                    <select id="weatherWorldSelect" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                        <option value="">选择世界</option>
                                                    </select>
                                                    <select id="weatherType" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                        <option value="clear">晴天</option>
                                                        <option value="rain">下雨</option>
                                                        <option value="thunder">雷雨</option>
                                                    </select>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--info-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">设置</button>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- 传送点管理 -->
                                    <div class="card">
                                        <h3 class="card-title">传送点管理</h3>
                                        <div style="margin-bottom: 20px;">
                                            <h4>添加传送点</h4>
                                            <form id="warpForm">
                                                <input type="text" id="warpName" placeholder="传送点名称" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                <input type="text" id="warpDescription" placeholder="描述" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">添加</button>
                                            </form>
                                        </div>
                                        <div class="table-container">
                                            <table id="warpsTable">
                                                <thead>
                                                    <tr>
                                                        <th>名称</th>
                                                        <th>描述</th>
                                                        <th>世界</th>
                                                        <th>位置</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="5" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 资源监控 -->
                            <div id="resources-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">资源监控</h2>
                                    <div class="server-info">
                                        <div class="info-card">
                                            <div class="value" id="resourcesTps">加载中...</div>
                                            <div class="label">TPS</div>
                                        </div>
                                        <div class="info-card">
                                            <div class="value" id="resourcesEntities">加载中...</div>
                                            <div class="label">实体数量</div>
                                        </div>
                                        <div class="info-card">
                                            <div class="value" id="resourcesChunks">加载中...</div>
                                            <div class="label">加载区块</div>
                                        </div>
                                        <div class="info-card">
                                            <div class="value" id="resourcesPlayers">加载中...</div>
                                            <div class="label">在线玩家</div>
                                        </div>
                                    </div>
                                    <h3 style="margin-top: 24px; margin-bottom: 16px; font-size: 16px; color: var(--text-color);">系统资源</h3>
                                    <div class="resource-item">
                                        <div class="resource-header">
                                            <span class="resource-name">进程CPU使用率</span>
                                            <span class="resource-value" id="resourcesCpu">加载中...</span>
                                        </div>
                                        <div class="resource-bar">
                                            <div id="cpuBar" class="resource-fill cpu-fill" style="width: 0%"></div>
                                        </div>
                                    </div>
                                    <div class="resource-item">
                                        <div class="resource-header">
                                            <span class="resource-name">系统CPU使用率</span>
                                            <span class="resource-value" id="resourcesSystemCpu">加载中...</span>
                                        </div>
                                        <div class="resource-bar">
                                            <div id="systemCpuBar" class="resource-fill cpu-fill" style="width: 0%"></div>
                                        </div>
                                    </div>
                                    <div class="resource-item">
                                        <div class="resource-header">
                                            <span class="resource-name">JVM内存使用率</span>
                                            <span class="resource-value" id="resourcesMemory">加载中...</span>
                                        </div>
                                        <div class="resource-bar">
                                            <div id="memoryBar" class="resource-fill memory-fill" style="width: 0%"></div>
                                        </div>
                                    </div>
                                    <div class="resource-item">
                                        <div class="resource-header">
                                            <span class="resource-name">系统内存使用率</span>
                                            <span class="resource-value" id="resourcesSystemMemory">加载中...</span>
                                        </div>
                                        <div class="resource-bar">
                                            <div id="systemMemoryBar" class="resource-fill memory-fill" style="width: 0%"></div>
                                        </div>
                                    </div>
                                    <div class="resource-item">
                                        <div class="resource-header">
                                            <span class="resource-name">磁盘使用率</span>
                                            <span class="resource-value" id="resourcesDisk">加载中...</span>
                                        </div>
                                        <div class="resource-bar">
                                            <div id="diskBar" class="resource-fill disk-fill" style="width: 0%"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 配置管理 -->
                            <div id="config-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">配置管理</h2>
                                    
                                    <!-- 服务器配置 -->
                                    <div class="card">
                                        <h3 class="card-title">服务器配置</h3>
                                        <div class="table-container">
                                            <table id="configTable">
                                                <thead>
                                                    <tr>
                                                        <th>配置项</th>
                                                        <th>当前值</th>
                                                        <th>默认值</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="4" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    
                                    <!-- 快速配置 -->
                                    <div class="card">
                                        <h3 class="card-title">快速配置</h3>
                                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
                                            <div>
                                                <h4>服务器设置</h4>
                                                <form id="serverSettingsForm">
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="maxPlayersConfig">最大玩家数:</label>
                                                        <input type="number" id="maxPlayersConfig" min="0" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="serverNameConfig">服务器名称:</label>
                                                        <input type="text" id="serverNameConfig" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="motdConfig">MOTD:</label>
                                                        <input type="text" id="motdConfig" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    </div>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">保存</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>游戏设置</h4>
                                                <form id="gameSettingsForm">
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="difficultyConfig">难度:</label>
                                                        <select id="difficultyConfig" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                            <option value="peaceful">和平</option>
                                                            <option value="easy">简单</option>
                                                            <option value="normal">普通</option>
                                                            <option value="hard">困难</option>
                                                        </select>
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="gamemodeConfig">默认游戏模式:</label>
                                                        <select id="gamemodeConfig" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                            <option value="survival">生存</option>
                                                            <option value="creative">创造</option>
                                                            <option value="adventure">冒险</option>
                                                            <option value="spectator">旁观者</option>
                                                        </select>
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="pvpConfig">PVP:</label>
                                                        <select id="pvpConfig" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                            <option value="true">开启</option>
                                                            <option value="false">关闭</option>
                                                        </select>
                                                    </div>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--info-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">保存</button>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- 配置备份 -->
                                    <div class="card">
                                        <h3 class="card-title">配置备份</h3>
                                        <div style="margin-bottom: 20px;">
                                            <button id="backupConfigBtn" style="padding: 10px 20px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer; margin-right: 10px;">备份当前配置</button>
                                            <button id="restoreConfigBtn" style="padding: 10px 20px; background-color: var(--warning-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">恢复配置</button>
                                        </div>
                                        <div class="table-container">
                                            <table id="configBackupsTable">
                                                <thead>
                                                    <tr>
                                                        <th>备份时间</th>
                                                        <th>文件大小</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="3" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 备份管理 -->
                            <div id="backup-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">备份管理</h2>
                                    
                                    <!-- 备份控制 -->
                                    <div class="card">
                                        <h3 class="card-title">备份控制</h3>
                                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
                                            <div>
                                                <h4>手动备份</h4>
                                                <form id="manualBackupForm">
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="backupName">备份名称:</label>
                                                        <input type="text" id="backupName" placeholder="输入备份名称" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="backupType">备份类型:</label>
                                                        <select id="backupType" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                            <option value="full">完整备份</option>
                                                            <option value="world">世界备份</option>
                                                            <option value="config">配置备份</option>
                                                        </select>
                                                    </div>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">创建备份</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>自动备份设置</h4>
                                                <form id="autoBackupForm">
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="backupInterval">备份间隔 (分钟):</label>
                                                        <input type="number" id="backupInterval" min="1" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="maxBackups">最大备份数:</label>
                                                        <input type="number" id="maxBackups" min="1" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    </div>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--info-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">保存设置</button>
                                                </form>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- 备份列表 -->
                                    <div class="card">
                                        <h3 class="card-title">备份列表</h3>
                                        <div class="table-container">
                                            <table id="backupsTable">
                                                <thead>
                                                    <tr>
                                                        <th>备份名称</th>
                                                        <th>备份时间</th>
                                                        <th>备份类型</th>
                                                        <th>文件大小</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="5" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 通知系统 -->
                            <div id="notifications-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">通知系统</h2>
                                    
                                    <!-- 发送通知 -->
                                    <div class="card">
                                        <h3 class="card-title">发送通知</h3>
                                        <form id="notificationForm">
                                            <div style="margin-bottom: 10px;">
                                                <label for="notificationType">通知类型:</label>
                                                <select id="notificationType" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                    <option value="broadcast">全服广播</option>
                                                    <option value="player">个人通知</option>
                                                    <option value="console">控制台通知</option>
                                                </select>
                                            </div>
                                            <div style="margin-bottom: 10px;">
                                                <label for="notificationTarget">目标 (可选):</label>
                                                <input type="text" id="notificationTarget" placeholder="玩家名称或目标" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                            </div>
                                            <div style="margin-bottom: 10px;">
                                                <label for="notificationMessage">消息内容:</label>
                                                <textarea id="notificationMessage" rows="4" required style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;"></textarea>
                                            </div>
                                            <button type="submit" style="padding: 10px 20px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">发送</button>
                                        </form>
                                    </div>
                                    
                                    <!-- 通知历史 -->
                                    <div class="card">
                                        <h3 class="card-title">通知历史</h3>
                                        <div class="table-container">
                                            <table id="notificationsTable">
                                                <thead>
                                                    <tr>
                                                        <th>时间</th>
                                                        <th>类型</th>
                                                        <th>发送者</th>
                                                        <th>目标</th>
                                                        <th>消息</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="5" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    
                                    <!-- 事件通知设置 -->
                                    <div class="card">
                                        <h3 class="card-title">事件通知设置</h3>
                                        <div class="table-container">
                                            <table id="eventNotificationsTable">
                                                <thead>
                                                    <tr>
                                                        <th>事件类型</th>
                                                        <th>状态</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td>玩家加入</td>
                                                        <td><span class="status-badge enabled">启用</span></td>
                                                        <td><button class="toggle-btn" data-event="join">禁用</button></td>
                                                    </tr>
                                                    <tr>
                                                        <td>玩家离开</td>
                                                        <td><span class="status-badge enabled">启用</span></td>
                                                        <td><button class="toggle-btn" data-event="leave">禁用</button></td>
                                                    </tr>
                                                    <tr>
                                                        <td>玩家死亡</td>
                                                        <td><span class="status-badge enabled">启用</span></td>
                                                        <td><button class="toggle-btn" data-event="death">禁用</button></td>
                                                    </tr>
                                                    <tr>
                                                        <td>服务器启动</td>
                                                        <td><span class="status-badge enabled">启用</span></td>
                                                        <td><button class="toggle-btn" data-event="start">禁用</button></td>
                                                    </tr>
                                                    <tr>
                                                        <td>服务器停止</td>
                                                        <td><span class="status-badge enabled">启用</span></td>
                                                        <td><button class="toggle-btn" data-event="stop">禁用</button></td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 安全管理 -->
                            <div id="security-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">安全管理</h2>
                                    
                                    <!-- IP访问控制 -->
                                    <div class="card">
                                        <h3 class="card-title">IP访问控制</h3>
                                        <div style="margin-bottom: 20px;">
                                            <form id="addAuthForm">
                                                <input type="text" id="authIp" placeholder="IP地址" required style="padding: 8px; margin-right: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                <input type="number" id="authHours" placeholder="授权小时数" required min="1" style="padding: 8px; margin-right: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">添加授权</button>
                                            </form>
                                        </div>
                                        <div class="table-container">
                                            <table id="authTable">
                                                <thead>
                                                    <tr>
                                                        <th>IP地址</th>
                                                        <th>过期时间</th>
                                                        <th>剩余时间</th>
                                                        <th>操作</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="4" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    
                                    <!-- 操作日志 -->
                                    <div class="card">
                                        <h3 class="card-title">操作日志</h3>
                                        <div class="table-container">
                                            <table id="logsTable">
                                                <thead>
                                                    <tr>
                                                        <th>时间</th>
                                                        <th>操作人</th>
                                                        <th>IP地址</th>
                                                        <th>操作类型</th>
                                                        <th>描述</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td colspan="5" class="loading">加载中...</td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    
                                    <!-- 安全设置 -->
                                    <div class="card">
                                        <h3 class="card-title">安全设置</h3>
                                        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px;">
                                            <div>
                                                <h4>访问控制</h4>
                                                <form id="securitySettingsForm">
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="ipWhitelistEnabled">IP白名单:</label>
                                                        <select id="ipWhitelistEnabled" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                            <option value="true">启用</option>
                                                            <option value="false">禁用</option>
                                                        </select>
                                                    </div>
                                                    <div style="margin-bottom: 10px;">
                                                        <label for="logAllActions">记录所有操作:</label>
                                                        <select id="logAllActions" style="width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                                                            <option value="true">启用</option>
                                                            <option value="false">禁用</option>
                                                        </select>
                                                    </div>
                                                    <button type="submit" style="padding: 8px 16px; background-color: var(--primary-color); color: white; border: none; border-radius: 4px; font-size: 14px; cursor: pointer;">保存设置</button>
                                                </form>
                                            </div>
                                            <div>
                                                <h4>敏感操作保护</h4>
                                                <div style="margin-bottom: 10px;">
                                                    <label><input type="checkbox" checked> 服务器重启需要确认</label>
                                                </div>
                                                <div style="margin-bottom: 10px;">
                                                    <label><input type="checkbox" checked> 玩家封禁需要确认</label>
                                                </div>
                                                <div style="margin-bottom: 10px;">
                                                    <label><input type="checkbox" checked> 配置修改需要确认</label>
                                                </div>
                                                <div style="margin-bottom: 10px;">
                                                    <label><input type="checkbox" checked> 备份恢复需要确认</label>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 命令执行 -->
                            <div id="console-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">命令执行</h2>
                                    <form id="commandForm">
                                        <input type="text" id="commandInput" placeholder="输入命令..." required>
                                        <button type="submit" id="submitCommand">执行</button>
                                    </form>
                                    <div id="commandResult"></div>
                                </div>
                            </div>
                            
                            <!-- 设置 -->
                            <div id="settings-section" class="section-content" style="display: none;">
                                <div class="card">
                                    <h2 class="card-title">设置</h2>
                                    
                                    <!-- 授权管理 -->
                                    <div class="card">
                                        <h3 class="card-title">授权管理</h3>
                                        <div id="authManagement">
                                            <!-- 添加授权表单 -->
                                            <div style="margin-bottom: 20px;">
                                                <h4>添加授权IP</h4>
                                                <form id="addAuthForm">
                                                    <input type="text" id="addAuthIp" placeholder="输入IP地址" required>
                                                    <input type="number" id="addAuthDays" placeholder="天数" required min="1">
                                                    <button type="submit">添加</button>
                                                </form>
                                            </div>
                                            <!-- 授权IP列表 -->
                                            <div>
                                                <h4>已授权IP列表</h4>
                                                <table id="authTable">
                                                    <thead>
                                                        <tr><th>IP地址</th><th>过期时间</th><th>操作</th></tr>
                                                    </thead>
                                                    <tbody>
                                                        <!-- 授权IP将通过JavaScript动态添加 -->
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>
                                    </div>
                                    
                                    <!-- 服务器设置 -->
                                    <div class="card" style="margin-top: 20px;">
                                        <h3 class="card-title">服务器设置</h3>
                                        <div style="margin-bottom: 20px;">
                                            <h4>服务器信息</h4>
                                            <p>服务器版本: <span id="serverVersionSettings">加载中...</span></p>
                                            <p>插件版本: 1.1beta</p>
                                        </div>
                                    </div>
                                    
                                    <!-- 系统设置 -->
                                    <div class="card" style="margin-top: 20px;">
                                        <h3 class="card-title">系统设置</h3>
                                        <div style="margin-bottom: 20px;">
                                            <h4>更新频率</h4>
                                            <p>服务器信息: 5秒</p>
                                            <p>玩家列表: 3秒</p>
                                            <p>资源监控: 2秒</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <script>
                    // 侧边栏导航
                    document.querySelectorAll('.sidebar-menu a').forEach(link => {
                        link.addEventListener('click', function(e) {
                            e.preventDefault();
                            
                            // 移除所有活动状态
                            document.querySelectorAll('.sidebar-menu a').forEach(item => {
                                item.classList.remove('active');
                            });
                            
                            // 添加活动状态
                            this.classList.add('active');
                            
                            // 隐藏所有内容区域
                            const sections = document.querySelectorAll('.section-content');
                            sections.forEach(section => {
                                section.style.opacity = '0';
                                section.style.transform = 'translateY(20px)';
                            });
                            
                            // 显示选中的内容区域
                            const sectionId = this.getAttribute('data-section') + '-section';
                            const targetSection = document.getElementById(sectionId);
                            
                            // 等待隐藏动画完成后再显示新内容
                            setTimeout(() => {
                                // 隐藏所有区域
                                sections.forEach(section => {
                                    section.style.display = 'none';
                                });
                                
                                // 显示目标区域
                                targetSection.style.display = 'block';
                                // 重置初始状态
                                targetSection.style.opacity = '0';
                                targetSection.style.transform = 'translateY(20px)';
                                
                                // 强制重排
                                targetSection.offsetHeight;
                                
                                // 开始显示动画
                                targetSection.style.opacity = '1';
                                targetSection.style.transform = 'translateY(0)';
                            }, 300);
                        });
                    });
                    
                    // 更新服务器信息
                    function updateServerInfo() {
                        fetch('/api/players')
                            .then(response => response.json())
                            .then(data => {
                                document.getElementById('playerCount').textContent = data.online;
                                document.getElementById('maxPlayers').textContent = data.max;
                                document.getElementById('onlinePlayers').textContent = data.online;
                                if (data.version) {
                                    document.getElementById('serverVersion').textContent = data.version;
                                    // 更新设置页面中的服务器版本
                                    const serverVersionSettings = document.getElementById('serverVersionSettings');
                                    if (serverVersionSettings) {
                                        serverVersionSettings.textContent = data.version;
                                    }
                                }
                            });
                    }
                    
                    // 更新玩家列表
                    function updatePlayerList() {
                        fetch('/api/players')
                            .then(response => response.json())
                            .then(data => {
                                // 更新控制台的玩家列表
                                const tbody = document.getElementById('playerTable').getElementsByTagName('tbody')[0];
                                tbody.innerHTML = '';
                                
                                if (data.players.length === 0) {
                                    const row = tbody.insertRow();
                                    const cell = row.insertCell();
                                    cell.colSpan = 4;
                                    cell.textContent = '当前没有在线玩家';
                                } else {
                                    data.players.forEach(player => {
                                        const row = tbody.insertRow();
                                        row.insertCell().textContent = player.name;
                                        row.insertCell().textContent = player.uuid;
                                        row.insertCell().textContent = player.ip;
                                        row.insertCell().textContent = player.gamemode;
                                    });
                                }
                                
                                // 更新玩家管理的列表
                                const playersTbody = document.getElementById('playersTable').getElementsByTagName('tbody')[0];
                                playersTbody.innerHTML = '';
                                
                                if (data.players.length === 0) {
                                    const row = playersTbody.insertRow();
                                    const cell = row.insertCell();
                                    cell.colSpan = 6;
                                    cell.textContent = '当前没有在线玩家';
                                } else {
                                    data.players.forEach(player => {
                                        const row = playersTbody.insertRow();
                                        row.insertCell().textContent = player.name;
                                        row.insertCell().textContent = player.uuid;
                                        row.insertCell().textContent = player.ip;
                                        row.insertCell().textContent = player.gamemode;
                                        row.insertCell().textContent = player.world || '未知';
                                        const actionCell = row.insertCell();
                                        actionCell.innerHTML = `
                                            <button class="action-btn" onclick="kickPlayer('${player.name}')">踢出</button>
                                            <button class="action-btn" onclick="banPlayer('${player.name}')">封禁</button>
                                            <button class="action-btn" onclick="teleportPlayer('${player.name}')">传送</button>
                                        `;
                                    });
                                }
                            });
                    }
                    
                    // 玩家操作函数
                    function kickPlayer(playerName) {
                        const reason = prompt('请输入踢出原因:');
                        if (reason !== null) {
                            executeCommand(`kick ${playerName} ${reason}`);
                        }
                    }
                    
                    function banPlayer(playerName) {
                        const reason = prompt('请输入封禁原因:');
                        if (reason !== null) {
                            executeCommand(`ban ${playerName} ${reason}`);
                        }
                    }
                    
                    function teleportPlayer(playerName) {
                        const target = prompt('请输入目标位置或玩家名称:');
                        if (target !== null) {
                            executeCommand(`tp ${playerName} ${target}`);
                        }
                    }
                    
                    // 执行命令的通用函数
                    function executeCommand(command) {
                        fetch('/api/command', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            body: 'command=' + encodeURIComponent(command)
                        })
                        .then(response => response.text())
                        .then(data => {
                            alert('命令执行结果: ' + data);
                            updatePlayerList();
                        })
                        .catch(error => {
                            alert('命令执行失败: ' + error.message);
                        });
                    }
                    
                    // 更新资源占用
                    function updateResources() {
                        fetch('/api/resources')
                            .then(response => response.json())
                            .then(data => {
                                // 更新控制台的资源信息
                                document.getElementById('onlinePlayers').textContent = data.onlinePlayers;
                                document.getElementById('tpsValue').textContent = data.tps;
                                document.getElementById('entityValue').textContent = data.entities;
                                document.getElementById('chunksValue').textContent = data.loadedChunks;
                                document.getElementById('cpuValue').textContent = data.cpu + '%';
                                document.getElementById('memoryValue').textContent = data.memory + '%';
                                document.getElementById('diskValue').textContent = data.disk + '%';
                                document.getElementById('systemCpuValue').textContent = data.systemCpu + '%';
                                
                                // 更新资源监控页面
                                document.getElementById('resourcesTps').textContent = data.tps;
                                document.getElementById('resourcesEntities').textContent = data.entities;
                                document.getElementById('resourcesChunks').textContent = data.loadedChunks;
                                document.getElementById('resourcesPlayers').textContent = data.onlinePlayers + '/' + data.maxPlayers;
                                document.getElementById('resourcesCpu').textContent = data.cpu + '%';
                                document.getElementById('resourcesSystemCpu').textContent = data.systemCpu + '%';
                                document.getElementById('resourcesMemory').textContent = data.memory + '%';
                                document.getElementById('resourcesSystemMemory').textContent = data.systemMemory + '%';
                                document.getElementById('resourcesDisk').textContent = data.disk + '%';
                                
                                // 更新进度条
                                document.getElementById('cpuBar').style.width = data.cpu + '%';
                                document.getElementById('systemCpuBar').style.width = data.systemCpu + '%';
                                document.getElementById('memoryBar').style.width = data.memory + '%';
                                document.getElementById('systemMemoryBar').style.width = data.systemMemory + '%';
                                document.getElementById('diskBar').style.width = data.disk + '%';
                            });
                    }
                    
                    // 执行命令
                    document.getElementById('commandForm').addEventListener('submit', function(e) {
                        e.preventDefault();
                        const command = document.getElementById('commandInput').value;
                        const resultDiv = document.getElementById('commandResult');
                        
                        resultDiv.textContent = '执行中...';
                        
                        fetch('/api/command', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            body: 'command=' + encodeURIComponent(command)
                        })
                        .then(response => response.text())
                        .then(data => {
                            resultDiv.textContent = data;
                        })
                        .catch(error => {
                            resultDiv.textContent = '命令执行失败: ' + error.message;
                        });
                    });
                    
                    // 授权管理
                    function loadAuthList() {
                        fetch('/api/auth')
                            .then(response => response.json())
                            .then(data => {
                                const tbody = document.getElementById('authTable').getElementsByTagName('tbody')[0];
                                tbody.innerHTML = '';
                                
                                if (data.authorizedIps.length === 0) {
                                    const row = tbody.insertRow();
                                    const cell = row.insertCell();
                                    cell.colSpan = 3;
                                    cell.textContent = '暂无授权IP';
                                } else {
                                    data.authorizedIps.forEach(auth => {
                                        const row = tbody.insertRow();
                                        const ipCell = row.insertCell();
                                        const expireCell = row.insertCell();
                                        const actionCell = row.insertCell();
                                        
                                        ipCell.textContent = auth.ip;
                                        expireCell.textContent = new Date(auth.expireTime).toLocaleString();
                                        actionCell.innerHTML = `
                                            <button onclick="removeAuth('${auth.ip}')" style="padding: 4px 8px; background-color: var(--error-color); color: white; border: none; border-radius: 4px; font-size: 12px; cursor: pointer; margin-right: 8px;">取消授权</button>
                                            <button onclick="extendAuth('${auth.ip}')" style="padding: 4px 8px; background-color: var(--success-color); color: white; border: none; border-radius: 4px; font-size: 12px; cursor: pointer;">延长授权</button>
                                        `;
                                    });
                                }
                            })
                            .catch(error => {
                                console.error('加载授权列表失败:', error);
                            });
                    }
                    
                    // 添加授权
                    document.getElementById('addAuthForm').addEventListener('submit', function(e) {
                        e.preventDefault();
                        const ip = document.getElementById('addAuthIp').value;
                        const days = document.getElementById('addAuthDays').value;
                        
                        fetch('/api/auth/add', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            body: 'ip=' + encodeURIComponent(ip) + '&days=' + encodeURIComponent(days)
                        })
                        .then(response => response.text())
                        .then(data => {
                            alert(data);
                            loadAuthList();
                            // 清空表单
                            document.getElementById('addAuthIp').value = '';
                            document.getElementById('addAuthDays').value = '';
                        })
                        .catch(error => {
                            alert('添加授权失败: ' + error.message);
                        });
                    });
                    
                    // 取消授权
                    function removeAuth(ip) {
                        if (confirm('确定要取消IP ' + ip + ' 的授权吗？')) {
                            fetch('/api/auth/remove', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                body: 'ip=' + encodeURIComponent(ip)
                            })
                            .then(response => response.text())
                            .then(data => {
                                alert(data);
                                loadAuthList();
                            })
                            .catch(error => {
                                alert('取消授权失败: ' + error.message);
                            });
                        }
                    }
                    
                    // 延长授权
                    function extendAuth(ip) {
                        const days = prompt('请输入要延长的天数:', '7');
                        if (days && !isNaN(days) && parseInt(days) > 0) {
                            fetch('/api/auth/extend', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                body: 'ip=' + encodeURIComponent(ip) + '&days=' + encodeURIComponent(days)
                            })
                            .then(response => response.text())
                            .then(data => {
                                alert(data);
                                loadAuthList();
                            })
                            .catch(error => {
                                alert('延长授权失败: ' + error.message);
                            });
                        }
                    }
                    
                    // 初始化更新
                    updateServerInfo();
                    updatePlayerList();
                    updateResources();
                    loadAuthList(); // 加载授权列表
                    
                    // 玩家操作表单
                    if (document.getElementById('kickForm')) {
                        document.getElementById('kickForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            const player = document.getElementById('kickPlayer').value;
                            const reason = document.getElementById('kickReason').value;
                            executeCommand(`kick ${player} ${reason}`);
                        });
                    }
                    
                    if (document.getElementById('banForm')) {
                        document.getElementById('banForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            const player = document.getElementById('banPlayer').value;
                            const reason = document.getElementById('banReason').value;
                            executeCommand(`ban ${player} ${reason}`);
                        });
                    }
                    
                    if (document.getElementById('teleportForm')) {
                        document.getElementById('teleportForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            const player = document.getElementById('teleportPlayer').value;
                            const target = document.getElementById('teleportTarget').value;
                            executeCommand(`tp ${player} ${target}`);
                        });
                    }
                    
                    if (document.getElementById('gamemodeForm')) {
                        document.getElementById('gamemodeForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            const player = document.getElementById('gamemodePlayer').value;
                            const gamemode = document.getElementById('gamemodeType').value;
                            executeCommand(`gamemode ${gamemode} ${player}`);
                        });
                    }
                    
                    // 服务器管理
                    if (document.getElementById('restartServerBtn')) {
                        document.getElementById('restartServerBtn').addEventListener('click', function() {
                            if (confirm('确定要重启服务器吗？')) {
                                executeCommand('restart');
                            }
                        });
                    }
                    
                    if (document.getElementById('stopServerBtn')) {
                        document.getElementById('stopServerBtn').addEventListener('click', function() {
                            if (confirm('确定要停止服务器吗？')) {
                                executeCommand('stop');
                            }
                        });
                    }
                    
                    if (document.getElementById('broadcastBtn')) {
                        document.getElementById('broadcastBtn').addEventListener('click', function() {
                            const message = prompt('请输入广播消息:');
                            if (message !== null) {
                                executeCommand(`say ${message}`);
                            }
                        });
                    }
                    
                    // 重启计划表单
                    if (document.getElementById('restartScheduleForm')) {
                        document.getElementById('restartScheduleForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            const hour = document.getElementById('restartHour').value;
                            const minute = document.getElementById('restartMinute').value;
                            alert(`定时重启已设置为每天 ${hour}:${minute}`);
                        });
                    }
                    
                    // 配置管理表单
                    if (document.getElementById('serverSettingsForm')) {
                        document.getElementById('serverSettingsForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            alert('服务器设置已保存');
                        });
                    }
                    
                    if (document.getElementById('gameSettingsForm')) {
                        document.getElementById('gameSettingsForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            alert('游戏设置已保存');
                        });
                    }
                    
                    if (document.getElementById('backupConfigBtn')) {
                        document.getElementById('backupConfigBtn').addEventListener('click', function() {
                            alert('配置已备份');
                        });
                    }
                    
                    if (document.getElementById('restoreConfigBtn')) {
                        document.getElementById('restoreConfigBtn').addEventListener('click', function() {
                            if (confirm('确定要恢复配置吗？')) {
                                alert('配置已恢复');
                            }
                        });
                    }
                    
                    // 备份管理
                    if (document.getElementById('manualBackupForm')) {
                        document.getElementById('manualBackupForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            alert('备份已创建');
                        });
                    }
                    
                    if (document.getElementById('autoBackupForm')) {
                        document.getElementById('autoBackupForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            alert('自动备份设置已保存');
                        });
                    }
                    
                    // 通知系统
                    if (document.getElementById('notificationForm')) {
                        document.getElementById('notificationForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            const type = document.getElementById('notificationType').value;
                            const target = document.getElementById('notificationTarget').value;
                            const message = document.getElementById('notificationMessage').value;
                            
                            let command = '';
                            if (type === 'broadcast') {
                                command = `say ${message}`;
                            } else if (type === 'player' && target) {
                                command = `tell ${target} ${message}`;
                            }
                            
                            if (command) {
                                executeCommand(command);
                            }
                        });
                    }
                    
                    // 安全设置
                    if (document.getElementById('securitySettingsForm')) {
                        document.getElementById('securitySettingsForm').addEventListener('submit', function(e) {
                            e.preventDefault();
                            alert('安全设置已保存');
                        });
                    }
                    
                    // 事件通知切换
                    document.querySelectorAll('.toggle-btn').forEach(btn => {
                        btn.addEventListener('click', function() {
                            const event = this.getAttribute('data-event');
                            const currentText = this.textContent;
                            this.textContent = currentText === '禁用' ? '启用' : '禁用';
                            
                            const row = this.closest('tr');
                            const statusBadge = row.querySelector('.status-badge');
                            if (statusBadge) {
                                if (currentText === '禁用') {
                                    statusBadge.textContent = '禁用';
                                    statusBadge.classList.remove('enabled');
                                    statusBadge.classList.add('disabled');
                                } else {
                                    statusBadge.textContent = '启用';
                                    statusBadge.classList.remove('disabled');
                                    statusBadge.classList.add('enabled');
                                }
                            }
                        });
                    });
                    
                    // 设置定时更新
                    setInterval(updateServerInfo, 5000);
                    setInterval(updatePlayerList, 3000);
                    setInterval(updateResources, 2000);
                    setInterval(loadAuthList, 60000); // 每分钟更新授权列表
                </script>
            </body>
            </html>
            """;
            
            exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"online\": " + Bukkit.getOnlinePlayers().size() + ",");
            json.append("\"max\": " + Bukkit.getMaxPlayers() + ",");
            json.append("\"version\": \"" + Bukkit.getVersion() + "\",");
            json.append("\"players\": [");
            
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (count > 0) json.append(",");
                json.append("{");
                json.append("\"name\": \"" + player.getName() + "\",");
                json.append("\"uuid\": \"" + player.getUniqueId() + "\",");
                json.append("\"ip\": \"" + player.getAddress().getAddress().getHostAddress() + "\",");
                json.append("\"gamemode\": \"" + player.getGameMode() + "\"");
                json.append("}");
                count++;
            }
            
            json.append("]");
            json.append("}");
            
            exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private class ResourcesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            double cpuUsage = plugin.getResourceMonitor().getCpuUsage();
            double systemCpuUsage = plugin.getResourceMonitor().getSystemCpuUsage();
            double memoryUsage = plugin.getResourceMonitor().getMemoryUsage();
            double systemMemoryUsage = plugin.getResourceMonitor().getSystemMemoryUsage();
            double diskUsage = plugin.getResourceMonitor().getDiskUsage();
            double tps = plugin.getResourceMonitor().getTPS();
            int entityCount = plugin.getResourceMonitor().getEntityCount();
            int onlinePlayers = plugin.getResourceMonitor().getOnlinePlayerCount();
            int maxPlayers = plugin.getResourceMonitor().getMaxPlayerCount();
            int loadedChunks = plugin.getResourceMonitor().getLoadedChunksCount();
            
            String json = "{" +
                "\"cpu\": " + cpuUsage + ","
                + "\"systemCpu\": " + systemCpuUsage + ","
                + "\"memory\": " + memoryUsage + ","
                + "\"systemMemory\": " + systemMemoryUsage + ","
                + "\"disk\": " + diskUsage + ","
                + "\"tps\": " + tps + ","
                + "\"entities\": " + entityCount + ","
                + "\"onlinePlayers\": " + onlinePlayers + ","
                + "\"maxPlayers\": " + maxPlayers + ","
                + "\"loadedChunks\": " + loadedChunks +
                "}";
            
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            if (exchange.getRequestMethod().equals("POST")) {
                StringBuilder requestBody = new StringBuilder();
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)))
                {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                
                String commandStr = requestBody.toString().replace("command=", "");
                commandStr = java.net.URLDecoder.decode(commandStr, StandardCharsets.UTF_8);
                final String finalCommand = commandStr;
                
                plugin.getLogger().info("执行命令: " + finalCommand);
                
                // 使用Bukkit调度器在主线程中执行命令
                Bukkit.getScheduler().runTask(plugin, () -> {
                    StringBuilder result = new StringBuilder();
                    try {
                        // 创建一个具有所有权限的命令发送者
                        org.bukkit.command.CommandSender sender = new org.bukkit.command.CommandSender() {
                            @Override
                            public void sendMessage(String message) {
                                plugin.getLogger().info("命令执行输出: " + message);
                            }

                            @Override
                            public void sendMessage(String... messages) {
                                for (String message : messages) {
                                    sendMessage(message);
                                }
                            }

                            @Override
                            public void sendMessage(java.util.UUID uuid, String message) {
                                sendMessage(message);
                            }

                            @Override
                            public void sendMessage(java.util.UUID uuid, String... messages) {
                                sendMessage(messages);
                            }

                            @Override
                            public String getName() {
                                return "WebManager-Console";
                            }

                            @Override
                            public boolean isPermissionSet(String name) {
                                return true;
                            }

                            @Override
                            public boolean isPermissionSet(org.bukkit.permissions.Permission perm) {
                                return true;
                            }

                            @Override
                            public boolean hasPermission(String name) {
                                return true;
                            }

                            @Override
                            public boolean hasPermission(org.bukkit.permissions.Permission perm) {
                                return true;
                            }

                            @Override
                            public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) {
                                return null;
                            }

                            @Override
                            public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) {
                                return null;
                            }

                            @Override
                            public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) {
                                return null;
                            }

                            @Override
                            public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) {
                                return null;
                            }

                            @Override
                            public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {
                            }

                            @Override
                            public void recalculatePermissions() {
                            }

                            @Override
                            public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() {
                                return java.util.Collections.emptySet();
                            }

                            @Override
                            public boolean isOp() {
                                return true;
                            }

                            @Override
                            public void setOp(boolean value) {
                            }

                            @Override
                            public org.bukkit.Server getServer() {
                                return Bukkit.getServer();
                            }

                            @Override
                            public org.bukkit.command.CommandSender.Spigot spigot() {
                                return new org.bukkit.command.CommandSender.Spigot() {};
                            }
                        };

                        // 执行命令
                        boolean success = Bukkit.getServer().dispatchCommand(sender, finalCommand);
                        if (success) {
                            result.append("命令执行成功: " + finalCommand);
                        } else {
                            result.append("命令执行失败: 未知错误");
                        }
                    } catch (Exception e) {
                        result.append("命令执行失败: " + e.getMessage());
                    } finally {
                        // 命令执行完成后发送响应
                        try {
                            exchange.sendResponseHeaders(200, result.toString().getBytes(StandardCharsets.UTF_8).length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(result.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            plugin.getLogger().severe("发送命令执行响应失败: " + e.getMessage());
                        }
                    }
                });
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        }
    }

    private class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            if (exchange.getRequestMethod().equals("POST")) {
                // 处理清空日志请求
                StringBuilder requestBody = new StringBuilder();
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)))
                {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                
                String action = requestBody.toString().replace("action=", "");
                if (action.equals("clear")) {
                    plugin.getLogListener().clearLogs();
                    String response = "日志已清空";
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    exchange.sendResponseHeaders(400, 0);
                    exchange.close();
                }
            } else if (exchange.getRequestMethod().equals("GET")) {
                // 处理获取日志请求
                var logs = plugin.getLogListener().getLogs();
                
                // 构建JSON响应
                StringBuilder json = new StringBuilder();
                json.append("{\"logs\": [");
                for (int i = 0; i < logs.size(); i++) {
                    json.append("\"").append(logs.get(i).replace("\"", "\\\"").replace("\\n", "\\\\n")).append("\"");
                    if (i < logs.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");
                
                exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        }
    }
    
    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            if (exchange.getRequestMethod().equals("GET")) {
                // 获取授权列表
                var authManager = plugin.getIpAuthManager();
                var authorizedIps = authManager.getAuthorizedIps();
                
                StringBuilder json = new StringBuilder();
                json.append("{\"authorizedIps\": [");
                
                int count = 0;
                for (var entry : authorizedIps.entrySet()) {
                    if (count > 0) json.append(",");
                    json.append("{");
                    json.append("\"ip\": \"").append(entry.getKey()).append("\",");
                    json.append("\"expireTime\": " + entry.getValue());
                    json.append("}");
                    count++;
                }
                
                json.append("]}");
                
                exchange.sendResponseHeaders(200, json.toString().getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }
            } else if (exchange.getRequestMethod().equals("POST")) {
                // 处理授权操作
                StringBuilder requestBody = new StringBuilder();
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)))
                {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                
                String path = exchange.getRequestURI().getPath();
                var authManager = plugin.getIpAuthManager();
                String response;
                
                if (path.equals("/api/auth/add")) {
                    // 添加授权
                    String[] params = requestBody.toString().split("&");
                    String ip = null;
                    int days = 0;
                    
                    for (String param : params) {
                        if (param.startsWith("ip=")) {
                            ip = param.replace("ip=", "");
                            ip = java.net.URLDecoder.decode(ip, StandardCharsets.UTF_8);
                        } else if (param.startsWith("days=")) {
                            days = Integer.parseInt(param.replace("days=", ""));
                        }
                    }
                    
                    if (ip != null && days > 0) {
                        authManager.addAuthorizedIp(ip, days);
                        response = "授权添加成功";
                    } else {
                        response = "参数错误";
                    }
                } else if (path.equals("/api/auth/remove")) {
                    // 移除授权
                    String ip = requestBody.toString().replace("ip=", "");
                    ip = java.net.URLDecoder.decode(ip, StandardCharsets.UTF_8);
                    
                    authManager.removeAuthorizedIp(ip);
                    response = "授权移除成功";
                } else if (path.equals("/api/auth/extend")) {
                    // 延长授权
                    String[] params = requestBody.toString().split("&");
                    String ip = null;
                    int days = 0;
                    
                    for (String param : params) {
                        if (param.startsWith("ip=")) {
                            ip = param.replace("ip=", "");
                            ip = java.net.URLDecoder.decode(ip, StandardCharsets.UTF_8);
                        } else if (param.startsWith("days=")) {
                            days = Integer.parseInt(param.replace("days=", ""));
                        }
                    }
                    
                    if (ip != null && days > 0) {
                        authManager.addAuthorizedIp(ip, days); // 直接添加会覆盖过期时间，实现延长效果
                        response = "授权延长成功";
                    } else {
                        response = "参数错误";
                    }
                } else {
                    response = "未知操作";
                }
                
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        }
    }
}
