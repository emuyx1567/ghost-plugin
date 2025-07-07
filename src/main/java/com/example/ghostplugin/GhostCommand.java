package com.example.ghostplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GhostCommand implements CommandExecutor {

    private final GhostPlugin plugin;
    
    public GhostCommand(GhostPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ghost")) {
            // 只有OP可以使用此命令
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
                return true;
            }
            
            if (args.length > 0) {
                // 管理员模式：ghost <player>
                Player target = plugin.getServer().getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "找不到玩家: " + args[0]);
                    return true;
                }
                
                if (sender instanceof Player) {
                    plugin.toggleGhost(target);
                    sender.sendMessage(ChatColor.GREEN + "已切换 " + target.getName() + " 的隐身状态!");
                } else {
                    plugin.toggleGhost(target);
                    sender.sendMessage(ChatColor.GREEN + "已切换 " + target.getName() + " 的隐身状态!");
                }
                return true;
            }
            
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台请指定玩家: /ghost <player>");
                return true;
            }

            Player player = (Player) sender;
            plugin.toggleGhost(player);
            return true;
        }
        return false;
    }
}