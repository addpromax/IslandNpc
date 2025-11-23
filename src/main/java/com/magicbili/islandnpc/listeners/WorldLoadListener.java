package com.magicbili.islandnpc.listeners;

import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * 监听ASP的SlimeWorld加载事件
 * 当世界被加载时（玩家进入或其他原因），自动spawn该世界中的NPC
 */
public class WorldLoadListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    
    public WorldLoadListener(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlimeWorldLoad(LoadSlimeWorldEvent event) {
        World world = event.getSlimeWorld().getBukkitWorld();
        
        // 异步处理，避免阻塞主线程
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            respawnNPCsInWorld(world);
        });
    }
    
    private void respawnNPCsInWorld(World world) {
        if (world == null) {
            return;
        }
        
        // 检查使用哪个NPC管理器
        if (plugin.getNpcManager() == null && plugin.getFancyNpcManager() == null) {
            return;
        }
        
        // 从配置文件读取所有岛屿的NPC数据
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) {
            return;
        }
        
        int recreated = 0;
        
        // 遍历所有保存的岛屿
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                // 检查世界名称是否匹配
                if (worldName != null && worldName.equals(world.getName())) {
                    // 回到主线程创建NPC（必须在主线程）
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (plugin.getNpcManager() != null) {
                            plugin.getNpcManager().recreateNpcForIsland(islandUUID);
                        } else if (plugin.getFancyNpcManager() != null) {
                            plugin.getFancyNpcManager().recreateNpcForIsland(islandUUID);
                        }
                    });
                    recreated++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿UUID: " + key);
            }
        }
        
        if (recreated > 0) {
            final int finalCount = recreated;
            plugin.getLogger().info("在世界 " + world.getName() + " 中重新创建了 " + finalCount + " 个NPC");
        }
    }
}
