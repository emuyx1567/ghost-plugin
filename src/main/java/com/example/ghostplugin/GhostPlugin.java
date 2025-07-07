package com.example.ghostplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GhostPlugin extends JavaPlugin implements Listener {
    
    private final Set<UUID> ghostPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BossBar> ghostBossBars = new HashMap<>();
    private Team ghostTeam;
    
    @Override
    public void onEnable() {
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册命令
        getCommand("ghost").setExecutor(new GhostCommand(this));
        
        // 设置隐身队伍
        setupGhostTeam();
        
        // 加载保存的ghost玩家
        loadGhostPlayers();
        
        getLogger().info(ChatColor.GREEN + "GhostPlugin 已启用! 版本: 3.0");
    }
    
    @Override
    public void onDisable() {
        // 保存ghost玩家状态
        saveGhostPlayers();
        
        // 移除所有boss栏
        for (BossBar bossBar : ghostBossBars.values()) {
            bossBar.removeAll();
        }
        ghostBossBars.clear();
        
        getLogger().info(ChatColor.RED + "GhostPlugin 已禁用");
    }
    
    private void setupGhostTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ghostTeam = scoreboard.getTeam("GHOST_TEAM");
        
        if (ghostTeam == null) {
            ghostTeam = scoreboard.registerNewTeam("GHOST_TEAM");
        }
        
        // 设置队伍属性使玩家完全不可见
        ghostTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        ghostTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        ghostTeam.setCanSeeFriendlyInvisibles(false);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 取消默认加入消息
        event.setJoinMessage(null);
        
        if (isGhost(player)) {
            // 对ghost玩家不显示加入消息
            applyGhostEffects(player, false);
            
            // 显示boss栏
            showBossBar(player);
            
            // 对新玩家隐藏所有ghost玩家（除了自己）
            for (UUID ghostId : ghostPlayers) {
                Player ghost = Bukkit.getPlayer(ghostId);
                if (ghost != null && ghost != player) {
                    player.hidePlayer(this, ghost);
                }
            }
        } else {
            // 对普通玩家显示加入消息
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != player && !isGhost(online)) {
                    online.sendMessage(ChatColor.YELLOW + player.getName() + " 加入了游戏");
                }
            }
            
            // 对普通玩家隐藏所有ghost玩家
            for (UUID ghostId : ghostPlayers) {
                Player ghost = Bukkit.getPlayer(ghostId);
                if (ghost != null) {
                    player.hidePlayer(this, ghost);
                    
                    // 对新玩家显示ghost玩家离开
                    player.sendMessage(ChatColor.YELLOW + ghost.getName() + " 离开了游戏");
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 取消退出消息
        event.setQuitMessage(null);
        
        // 如果是普通玩家，显示退出消息
        if (!isGhost(player)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != player && !isGhost(online)) {
                    online.sendMessage(ChatColor.YELLOW + player.getName() + " 离开了游戏");
                }
            }
        }
        
        // 移除boss栏
        removeBossBar(player);
    }
    
    public void toggleGhost(Player player) {
        if (isGhost(player)) {
            removeGhost(player);
            player.sendMessage(ChatColor.GREEN + "你已解除隐身状态!");
        } else {
            addGhost(player);
            player.sendMessage(ChatColor.GRAY + "你已进入隐身状态!");
            player.sendMessage(ChatColor.GRAY + "其他玩家将无法看到你或你在玩家列表中的信息");
        }
    }
    
    public void addGhost(Player player) {
        ghostPlayers.add(player.getUniqueId());
        applyGhostEffects(player, true);
        saveGhostPlayers();
        
        // 添加boss栏
        showBossBar(player);
    }
    
    public void removeGhost(Player player) {
        ghostPlayers.remove(player.getUniqueId());
        removeGhostEffects(player);
        saveGhostPlayers();
        
        // 移除boss栏
        removeBossBar(player);
    }
    
    public boolean isGhost(Player player) {
        return ghostPlayers.contains(player.getUniqueId());
    }
    
    private void applyGhostEffects(Player player, boolean notifyOthers) {
        // 加入隐身队伍
        if (!ghostTeam.hasEntry(player.getName())) {
            ghostTeam.addEntry(player.getName());
        }
        
        // 对所有在线玩家隐藏此玩家
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                // 普通玩家需要隐藏ghost玩家
                if (!isGhost(online) && online.canSee(player)) {
                    online.hidePlayer(this, player);
                    
                    // 通知其他玩家该玩家已离开
                    if (notifyOthers) {
                        online.sendMessage(ChatColor.YELLOW + player.getName() + " 离开了游戏");
                    }
                }
                // ghost玩家可以看到其他ghost玩家
                else if (isGhost(online) && !online.canSee(player)) {
                    online.showPlayer(this, player);
                }
            }
        }
    }
    
    private void removeGhostEffects(Player player) {
        // 从隐身队伍中移除
        if (ghostTeam.hasEntry(player.getName())) {
            ghostTeam.removeEntry(player.getName());
        }
        
        // 对所有在线玩家显示此玩家
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.canSee(player)) {
                online.showPlayer(this, player);
                
                // 通知其他玩家该玩家已加入
                if (online != player && !isGhost(online)) {
                    online.sendMessage(ChatColor.YELLOW + player.getName() + " 加入了游戏");
                }
            }
        }
    }
    
    private void showBossBar(Player player) {
        // 创建或更新boss栏
        BossBar bossBar = ghostBossBars.computeIfAbsent(
            player.getUniqueId(), 
            k -> Bukkit.createBossBar(
                ChatColor.RED + "隐身模式", 
                BarColor.PURPLE, 
                BarStyle.SOLID
            )
        );
        
        bossBar.setVisible(true);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
    }
    
    private void removeBossBar(Player player) {
        BossBar bossBar = ghostBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
            bossBar.setVisible(false);
        }
    }
    
    private void loadGhostPlayers() {
        // 实际应用中应从文件或数据库加载
        // 这里简化处理，实际使用时需要实现持久化存储
        getLogger().info("已加载 " + ghostPlayers.size() + " 个ghost玩家");
        
        // 为已加载的ghost玩家应用效果
        for (UUID uuid : ghostPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                applyGhostEffects(player, false);
                showBossBar(player);
            }
        }
    }
    
    private void saveGhostPlayers() {
        // 实际应用中应保存到文件或数据库
        // 这里简化处理，实际使用时需要实现持久化存储
        getLogger().info("已保存 " + ghostPlayers.size() + " 个ghost玩家");
    }
}