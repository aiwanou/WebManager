package com.webmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class WebManager extends JavaPlugin {

    private WebHttpServer httpServer;
    private ResourceMonitor resourceMonitor;
    private LogListener logListener;
    private IpAuthorizationManager ipAuthManager;
    private OperationLogger operationLogger;

    @Override
    public void onEnable() {
        getLogger().info("WebManager 插件已启用");
        
        // 初始化组件
        resourceMonitor = new ResourceMonitor();
        logListener = new LogListener();
        ipAuthManager = new IpAuthorizationManager(this);
        operationLogger = new OperationLogger(this);
        
        // 注册日志监听器
        logListener.register(this);
        
        // 注册命令
        getCommand("webmanager").setExecutor(this);
        
        // 启动HTTP服务器
        httpServer = new WebHttpServer(this);
        httpServer.start(9876);
        
        getLogger().info("WebManager HTTP服务器已启动在端口 9876");
        getLogger().info("管理界面地址: http://服务器IP:9876");
        getLogger().info("使用 /webmanager add <ip> <days> 授权IP访问");
    }

    @Override
    public void onDisable() {
        getLogger().info("WebManager 插件已禁用");
        
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("webmanager.admin")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§aWebManager 命令帮助:");
            sender.sendMessage("§a/webmanager add <ip> <days> - 授权IP访问管理界面");
            sender.sendMessage("§a/webmanager remove <ip> - 移除IP授权");
            sender.sendMessage("§a/webmanager list - 查看所有授权IP");
            sender.sendMessage("§a/webmanager reload - 重新加载插件");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length != 3) {
                    sender.sendMessage("§c用法: /webmanager add <ip> <days>");
                    return true;
                }
                try {
                    String ip = args[1];
                    int days = Integer.parseInt(args[2]);
                    if (days <= 0) {
                        sender.sendMessage("§c天数必须大于0");
                        return true;
                    }
                    ipAuthManager.addAuthorizedIp(ip, days);
                    sender.sendMessage("§a已授权IP " + ip + " 访问 " + days + " 天");
                    getLogger().info("管理员 " + sender.getName() + " 授权IP " + ip + " 访问 " + days + " 天");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c无效的天数");
                }
                break;
            case "remove":
                if (args.length != 2) {
                    sender.sendMessage("§c用法: /webmanager remove <ip>");
                    return true;
                }
                String removeIp = args[1];
                ipAuthManager.removeAuthorizedIp(removeIp);
                sender.sendMessage("§a已移除IP " + removeIp + " 的授权");
                getLogger().info("管理员 " + sender.getName() + " 移除了IP " + removeIp + " 的授权");
                break;
            case "list":
                sender.sendMessage("§a已授权的IP:");
                for (String ip : ipAuthManager.getAuthorizedIps().keySet()) {
                    sender.sendMessage("§a- " + ip);
                }
                break;
            case "reload":
                sender.sendMessage("§aWebManager 插件已重新加载");
                break;
            default:
                sender.sendMessage("§c未知命令，请使用 /webmanager 查看帮助");
                break;
        }

        return true;
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    public LogListener getLogListener() {
        return logListener;
    }

    public IpAuthorizationManager getIpAuthManager() {
        return ipAuthManager;
    }

    public OperationLogger getOperationLogger() {
        return operationLogger;
    }
}
