package com.magicbili.islandnpc.listeners;

import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.magicbili.islandnpc.IslandNpcPlugin;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.UUID;

/**
 * 监听世界加载和卸载事件
 * 加载时：自动重新创建该世界中的NPC
 * 卸载时：清理NPC和全息图的引用
 */
public class WorldLoadListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    
    public WorldLoadListener(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 输出debug日志（仅在debug模式启用时）
     */
    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * 监听普通世界加载事件（Bukkit 标准事件）
     * 用于在服务器重启后重新创建NPC
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        
        debug("[世界加载] 检测到世界 " + world.getName() + " 加载");
        
        // 延迟处理，确保世界完全加载（40 ticks = 2秒）
        // 给予更多时间让其他插件（如SuperiorSkyblock）完全初始化
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            respawnNPCsInWorld(world);
        }, 40L);
    }
    
    /**
     * 监听 SlimeWorld 加载事件（ASP 专用事件）
     * 用于 SlimeWorld 动态世界加载
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlimeWorldLoad(LoadSlimeWorldEvent event) {
        World world = event.getSlimeWorld().getBukkitWorld();
        
        debug("[SlimeWorld加载] 检测到 SlimeWorld " + world.getName() + " 加载");
        
        // 延迟处理，确保世界完全加载（20 ticks = 1秒）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            respawnNPCsInWorld(world);
        }, 20L);
    }
    
    /**
     * 监听世界卸载事件
     * 在世界卸载时清理NPC和全息图的引用，避免内存泄漏
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        debug("[世界卸载] 检测到世界 " + world.getName() + " 卸载");
        cleanupNPCsInWorld(world);
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
        
        debug("检测到世界 " + world.getName() + " 已加载，准备重新创建NPC...");
        
        int recreatedCount = 0;
        
        // 遍历所有保存的岛屿
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                // 检查世界名称是否匹配
                if (worldName != null && worldName.equals(world.getName())) {
                    debug("发现岛屿 " + islandUUID + " 的NPC需要重新创建");
                    
                    // 在主线程创建NPC（必须在主线程）
                    if (plugin.getNpcManager() != null) {
                        plugin.getNpcManager().recreateNpcForIsland(islandUUID);
                    } else if (plugin.getFancyNpcManager() != null) {
                        plugin.getFancyNpcManager().recreateNpcForIsland(islandUUID);
                    }
                    recreatedCount++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿UUID: " + key);
            }
        }
        
        if (recreatedCount > 0) {
            debug("世界 " + world.getName() + " 重新创建了 " + recreatedCount + " 个NPC");
        }
    }
    
    /**
     * 清理指定世界中的NPC和全息图引用
     */
    private void cleanupNPCsInWorld(World world) {
        if (world == null) {
            return;
        }
        
        // 检查使用哪个NPC管理器
        if (plugin.getNpcManager() == null && plugin.getFancyNpcManager() == null) {
            return;
        }
        
        debug("检测到世界 " + world.getName() + " 即将卸载，清理NPC引用...");
        
        // 从配置文件读取所有岛屿的NPC数据
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) {
            return;
        }
        
        int cleaned = 0;
        
        // 遍历所有保存的岛屿
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                // 检查世界名称是否匹配
                if (worldName != null && worldName.equals(world.getName())) {
                    // 使用FancyNpcManager
                    if (plugin.getFancyNpcManager() != null) {
                        // 获取NPC
                        Npc npc = plugin.getFancyNpcManager().getIslandNpc(islandUUID);
                        if (npc != null) {
                            try {
                                // 从FancyNpcs管理器中移除NPC（但不从我们的配置中删除）
                                FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
                                debug("已清理岛屿 " + islandUUID + " 的NPC引用");
                            } catch (Exception e) {
                                plugin.getLogger().warning("清理NPC失败: " + e.getMessage());
                            }
                        }
                    }
                    // Citizens的处理类似
                    else if (plugin.getNpcManager() != null) {
                        // Citizens的NPC会自动被清理，这里可以添加额外逻辑
                    }
                    
                    cleaned++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿UUID: " + key);
            }
        }
        
        if (cleaned > 0) {
            debug("世界 " + world.getName() + " 卸载时清理了 " + cleaned + " 个NPC的引用");
        }
    }
}
