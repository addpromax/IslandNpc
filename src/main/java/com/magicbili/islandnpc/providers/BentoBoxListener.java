package com.magicbili.islandnpc.providers;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.NpcProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.database.objects.Island;

import java.util.UUID;

/**
 * BentoBox 事件监听器
 * 
 * @author magicbili
 */
public class BentoBoxListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    private final NpcProvider npcProvider;
    
    public BentoBoxListener(IslandNpcPlugin plugin, NpcProvider npcProvider) {
        this.plugin = plugin;
        this.npcProvider = npcProvider;
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
     * 监听岛屿创建完成事件
     * 注意：BentoBox 使用 IslandCreatedEvent（已创建）而不是 IslandCreateEvent（创建中）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreated(IslandCreatedEvent event) {
        Island island = event.getIsland();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (island != null) {
                Location center = island.getProtectionCenter();
                if (center == null) {
                    center = island.getCenter();
                }
                
                if (center != null) {
                    UUID islandUUID = UUID.fromString(island.getUniqueId());
                    debug("岛屿创建: " + islandUUID);
                    createNpcForBentoBoxIsland(island);
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
        
        debug("玩家 " + event.getPlayerUUID() + " 进入岛屿: " + island.getUniqueId());
        
        // 不再在此处创建 NPC，由世界加载事件统一处理
    }
    
    /**
     * 监听岛屿删除事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent event) {
        Island island = event.getIsland();
        if (island != null) {
            UUID islandUUID = UUID.fromString(island.getUniqueId());
            debug("监听到岛屿删除事件: " + islandUUID);
            debug("岛屿拥有者: " + island.getOwner());
            
            // 异步删除NPC，避免阻塞主线程
            Bukkit.getScheduler().runTask(plugin, () -> {
                npcProvider.deleteNpc(islandUUID);
            });
        } else {
            debug("岛屿删除事件中的岛屿为 null");
        }
    }
    
    /**
     * 为BentoBox岛屿创建NPC
     */
    private void createNpcForBentoBoxIsland(Island bentoBoxIsland) {
        if (npcProvider == null) {
            debug("NPC提供者未初始化");
            return;
        }
        
        UUID islandUUID = UUID.fromString(bentoBoxIsland.getUniqueId());
        Location center = bentoBoxIsland.getProtectionCenter();
        if (center == null) {
            center = bentoBoxIsland.getCenter();
        }
        
        if (center == null) {
            debug("无法为岛屿 " + islandUUID + " 创建NPC: 中心位置为null");
            return;
        }
        
        Location spawnLoc = calculateSpawnLocation(center);
        npcProvider.createNpc(islandUUID, spawnLoc);
    }
    
    /**
     * 计算NPC生成位置
     */
    private Location calculateSpawnLocation(Location islandCenter) {
        if (islandCenter == null) return null;

        double offsetX = plugin.getConfigManager().getSpawnOffsetX();
        double offsetY = plugin.getConfigManager().getSpawnOffsetY();
        double offsetZ = plugin.getConfigManager().getSpawnOffsetZ();
        float yaw = plugin.getConfigManager().getNpcYaw();
        float pitch = plugin.getConfigManager().getNpcPitch();

        Location spawnLoc = islandCenter.clone().add(offsetX, offsetY, offsetZ);
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(pitch);
        
        return spawnLoc;
    }
}
