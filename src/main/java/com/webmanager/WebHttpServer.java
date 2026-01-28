package com.webmanager;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
            server.createContext("/api/operation-logs", new OperationLogsHandler());
            server.createContext("/api/worlds", new WorldsHandler());
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
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }

            // 从resources目录读取template.html文件
            String html = loadTemplate();
            
            exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes(StandardCharsets.UTF_8));
            }
        }

        private String loadTemplate() {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("template.html")) {
                if (is != null) {
                    byte[] buffer = new byte[is.available()];
                    is.read(buffer);
                    return new String(buffer, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("读取模板文件失败: " + e.getMessage());
            }
            return "<html><body><h1>错误: 无法加载模板文件</h1></body></html>";
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
            
            try {
                // 使用callSyncMethod在主线程中获取玩家数据
                final java.util.concurrent.atomic.AtomicReference<String> jsonRef = new java.util.concurrent.atomic.AtomicReference<>();
                org.bukkit.Bukkit.getScheduler().callSyncMethod(plugin, new java.util.concurrent.Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        StringBuilder json = new StringBuilder();
                        json.append("{");
                        json.append("\"online\": " + org.bukkit.Bukkit.getOnlinePlayers().size() + ",");
                        json.append("\"max\": " + org.bukkit.Bukkit.getMaxPlayers() + ",");
                        json.append("\"version\": \"" + org.bukkit.Bukkit.getVersion() + "\",");
                        json.append("\"players\": [");
                        
                        int count = 0;
                        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                            if (count > 0) json.append(",");
                            json.append("{");
                            json.append("\"name\": \"" + player.getName() + "\",");
                            json.append("\"uuid\": \"" + player.getUniqueId() + "\",");
                            if (player.getAddress() != null) {
                                json.append("\"ip\": \"" + player.getAddress().getAddress().getHostAddress() + "\",");
                            } else {
                                json.append("\"ip\": \"未知\",");
                            }
                            json.append("\"gamemode\": \"" + player.getGameMode() + "\"");
                            json.append("}");
                            count++;
                        }
                        
                        json.append("]");
                        json.append("}");
                        jsonRef.set(json.toString());
                        return null;
                    }
                }).get();
                
                String json = jsonRef.get();
                exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String errorJson = "{\"error\": \"获取玩家数据失败\"}";
                exchange.sendResponseHeaders(500, errorJson.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorJson.getBytes(StandardCharsets.UTF_8));
                }
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
                InputStream is = exchange.getRequestBody();
                byte[] buffer = new byte[exchange.getRequestBody().available()];
                is.read(buffer);
                String requestBody = new String(buffer, StandardCharsets.UTF_8);
                
                // 解析命令
                final String command = requestBody.split("=")[1];
                String decodedCommand = java.net.URLDecoder.decode(command, StandardCharsets.UTF_8.name());
                
                // 执行命令
                StringBuilder result = new StringBuilder();
                try {
                    final String finalCommand = decodedCommand;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    });
                    result.append("命令执行成功: " + decodedCommand);
                    // 记录操作日志
                    plugin.getOperationLogger().logOperation(clientIp, "命令执行", decodedCommand);
                } catch (Exception e) {
                    result.append("命令执行失败: " + e.getMessage());
                    // 记录操作日志
                    plugin.getOperationLogger().logOperation(clientIp, "命令执行", decodedCommand + " (失败: " + e.getMessage() + ")");
                }
                
                exchange.sendResponseHeaders(200, result.toString().getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(result.toString().getBytes(StandardCharsets.UTF_8));
                }
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
            
            java.util.List<String> logList = plugin.getLogListener().getRecentLogs(100);
            StringBuilder logs = new StringBuilder();
            for (String log : logList) {
                logs.append(log).append("\n");
            }
            exchange.sendResponseHeaders(200, logs.toString().getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(logs.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private class OperationLogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            java.util.List<String> logList = plugin.getOperationLogger().getRecentLogs(100);
            StringBuilder logs = new StringBuilder();
            for (String log : logList) {
                logs.append(log).append("\n");
            }
            exchange.sendResponseHeaders(200, logs.toString().getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(logs.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private class WorldsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 检查IP授权
            String clientIp = getClientIp(exchange);
            if (!isIpAuthorized(clientIp)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }
            
            try {
                // 使用callSyncMethod在主线程中获取世界数据
                final java.util.concurrent.atomic.AtomicReference<String> jsonRef = new java.util.concurrent.atomic.AtomicReference<>();
                org.bukkit.Bukkit.getScheduler().callSyncMethod(plugin, new java.util.concurrent.Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        StringBuilder json = new StringBuilder();
                        json.append("{");
                        json.append("\"worlds\": [");
                        
                        int count = 0;
                        for (org.bukkit.World world : org.bukkit.Bukkit.getServer().getWorlds()) {
                            if (count > 0) json.append(",");
                            json.append("{");
                            json.append("\"name\": \"" + world.getName() + "\",");
                            json.append("\"type\": \"" + world.getEnvironment() + "\",");
                            json.append("\"seed\": " + world.getSeed() + ",");
                            json.append("\"players\": " + world.getPlayers().size() + ",");
                            json.append("\"entities\": " + world.getEntities().size() + ",");
                            json.append("\"chunks\": " + world.getLoadedChunks().length);
                            json.append("}");
                            count++;
                        }
                        
                        json.append("]");
                        json.append("}");
                        jsonRef.set(json.toString());
                        return null;
                    }
                }).get();
                
                String json = jsonRef.get();
                exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String errorJson = "{\"error\": \"获取世界数据失败\"}";
                exchange.sendResponseHeaders(500, errorJson.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorJson.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }


}
