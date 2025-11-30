package com.magicbili.islandnpc.providers;

import com.bgsoftware.superiorskyblock.api.events.IslandCreateEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandEnterEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.NpcProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * SuperiorSkyblock2 事件监听器
 * 
 * @author magicbili
 */
public class SuperiorSkyblockListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    private final NpcProvider npcProvider;
    
    public SuperiorSkyblockListener(IslandNpcPlugin plugin, NpcProvider npcProvider) {
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
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreate(IslandCreateEvent event) {
        Island island = event.getIsland();
        
        debug("检测到岛屿创建事件: " + (island != null ? island.getUniqueId() : "null"));
        if (island != null) {
            debug("岛屿拥有者: " + (island.getOwner() != null ? island.getOwner().getName() : "未知"));
            debug("岛屿世界: " + (island.getCenter(Dimension.getByName("NORMAL")) != null ? 
                island.getCenter(Dimension.getByName("NORMAL")).getWorld().getName() : "未知"));
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (island != null && npcProvider != null) {
                debug("延迟任务执行: 开始为岛屿 " + island.getUniqueId() + " 创建 NPC");
                Dimension normalDimension = Dimension.getByName("NORMAL");
                Location center = island.getCenter(normalDimension);
                if (center != null) {
                    debug("岛屿中心位置: " + center);
                    Location spawnLoc = calculateSpawnLocation(center);
                    debug("NPC 生成位置: " + spawnLoc);
                    boolean success = npcProvider.createNpc(island.getUniqueId(), spawnLoc);
                    debug("NPC 创建结果: " + success);
                } else {
                    debug("岛屿中心位置为 null，无法创建 NPC");
                }
            } else {
                debug("延迟任务执行失败: island=" + (island != null) + ", npcProvider=" + (npcProvider != null));
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
        if (island != null && npcProvider != null) {
            debug("监听到岛屿删除事件: " + island.getUniqueId());
            debug("岛屿拥有者: " + 
                (island.getOwner() != null ? island.getOwner().getName() : "未知"));
            
            // 异步删除NPC，避免阻塞主线程
            Bukkit.getScheduler().runTask(plugin, () -> {
                npcProvider.deleteNpc(island.getUniqueId());
            });
        } else {
            debug("岛屿删除事件中的岛屿为 null");
        }
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
