package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.events.IslandCreateEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandEnterEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PlayerIslandListener implements Listener {

    private final IslandNpcPlugin plugin;

    public PlayerIslandListener(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 输出debug日志（仅在debug模式启用时）
     * @param message 日志消息
     */
    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreate(IslandCreateEvent event) {
        Island island = event.getIsland();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (island != null) {
                com.bgsoftware.superiorskyblock.api.world.Dimension normalDimension = 
                    com.bgsoftware.superiorskyblock.api.world.Dimension.getByName("NORMAL");
                if (island.getCenter(normalDimension) != null) {
                    // 根据配置使用对应的NPC管理器
                    if (plugin.getNpcManager() != null) {
                        plugin.getNpcManager().createIslandNpc(island);
                    } else if (plugin.getFancyNpcManager() != null) {
                        plugin.getFancyNpcManager().createIslandNpc(island);
                    }
                }
            }
        }, 20L);
    }
    
    /**
     * 监听玩家进入岛屿事件
     * 仅用于调试日志，NPC 的创建由世界加载事件处理
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandEnter(IslandEnterEvent event) {
        Island island = event.getIsland();
        if (island == null) {
            return;
        }
        
        debug("玩家 " + event.getPlayer().getName() + " 进入岛屿: " + island.getUniqueId());
        
        // 不再在此处创建 NPC，由世界加载事件统一处理
        // 这样可以避免重复创建的问题
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        Island island = event.getIsland();
        if (island != null) {
            debug("监听到岛屿删除事件: " + island.getUniqueId());
            debug("岛屿拥有者: " + 
                (island.getOwner() != null ? island.getOwner().getName() : "未知"));
            
            // 异步删除NPC，避免阻塞主线程
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 根据配置使用对应的NPC管理器
                if (plugin.getNpcManager() != null) {
                    debug("使用 CitizensNPC 管理器删除 NPC");
                    plugin.getNpcManager().deleteNpc(island.getUniqueId());
                } else if (plugin.getFancyNpcManager() != null) {
                    debug("使用 FancyNPC 管理器删除 NPC");
                    plugin.getFancyNpcManager().deleteNpc(island.getUniqueId());
                } else {
                    debug("没有可用的 NPC 管理器");
                }
            });
        } else {
            debug("岛屿删除事件中的岛屿为 null");
        }
    }
}
