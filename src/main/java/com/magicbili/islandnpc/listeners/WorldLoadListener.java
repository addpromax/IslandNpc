package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 监听世界加载和卸载事件
 * 加载时：使用 SuperiorSkyblock API 判断岛屿世界，使用 AdvancedSlimePaper API 判断 SlimeWorld
 *        自动重新创建该世界中的NPC（支持 SlimeWorld 和普通岛屿世界）
 * 卸载时：清理NPC和全息图的引用
 */
public class WorldLoadListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    private final Set<String> processingWorlds = new HashSet<>();
    
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
     * 监听世界加载事件（Bukkit 标准事件）
     * 主要用于普通岛屿世界的加载
     * SlimeWorld 由 LoadSlimeWorldEvent 专门处理
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();
        
        debug("[世界加载] 检测到世界加载: " + worldName);
        
        // 检查是否是岛屿世界
        if (!isIslandWorld(world)) {
            debug("[世界加载] 非岛屿世界，跳过");
            return;
        }
        
        // 防止与 SlimeWorld 事件重复处理
        if (!tryAddProcessingWorld(worldName)) {
            debug("[世界加载] 世界已在处理中，跳过");
            return;
        }
        
        // 延迟处理，确保世界完全加载（80 ticks = 4秒）
        scheduleNpcRespawn(worldName, 80L);
    }
    
    /**
     * 监听 SlimeWorld 加载事件（ASP 专用事件）
     * 用于 SlimeWorld 动态加载时自动创建 NPC
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlimeWorldLoad(LoadSlimeWorldEvent event) {
        World world = event.getSlimeWorld().getBukkitWorld();
        
        if (world == null) {
            plugin.getLogger().warning("LoadSlimeWorldEvent 中的 world 为 null，跳过处理");
            return;
        }
        
        String worldName = world.getName();
        debug("[SlimeWorld加载] 检测到加载: " + worldName);
        
        // 防止重复处理
        if (!tryAddProcessingWorld(worldName)) {
            debug("[SlimeWorld加载] 世界已在处理中，跳过");
            return;
        }
        
        // 延迟处理，确保世界完全加载（60 ticks = 3秒）
        scheduleNpcRespawn(worldName, 60L);
    }
    
    /**
     * 监听世界卸载事件
     * 清理NPC和全息图引用，避免内存泄漏
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        debug("[世界卸载] " + world.getName());
        cleanupNPCsInWorld(world);
    }
    
    /**
     * 检查世界是否是岛屿世界
     */
    private boolean isIslandWorld(World world) {
        try {
            Island island = SuperiorSkyblockAPI.getIslandAt(world.getSpawnLocation());
            return island != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 尝试添加世界到处理队列（原子操作）
     * @return true 如果成功添加，false 如果已在处理中
     */
    private boolean tryAddProcessingWorld(String worldName) {
        synchronized (processingWorlds) {
            return processingWorlds.add(worldName);
        }
    }
    
    /**
     * 安排延迟任务重新生成 NPC
     */
    private void scheduleNpcRespawn(String worldName, long delayTicks) {
        debug("安排延迟任务: " + delayTicks + " ticks 后处理世界 " + worldName);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    debug("延迟处理: 开始重新创建 NPC - " + worldName);
                    respawnNPCsInWorld(world);
                } else {
                    plugin.getLogger().warning("延迟处理时找不到世界: " + worldName);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("处理世界时发生错误: " + worldName + " - " + e.getMessage());
                e.printStackTrace();
            } finally {
                synchronized (processingWorlds) {
                    processingWorlds.remove(worldName);
                }
            }
        }, delayTicks);
    }
    
    /**
     * 重新生成指定世界中的所有 NPC
     */
    private void respawnNPCsInWorld(World world) {
        if (world == null) return;
        
        int count = processIslandsInWorld(world, (islandUUID) -> {
            if (plugin.getFancyNpcManager() != null) {
                plugin.getFancyNpcManager().recreateNpcForIsland(islandUUID);
            } else if (plugin.getNpcManager() != null) {
                plugin.getNpcManager().recreateNpcForIsland(islandUUID);
            }
        });
        
        if (count > 0) {
            debug("重新创建了 " + count + " 个 NPC");
        }
    }
    
    /**
     * 清理指定世界中的 NPC 引用
     */
    private void cleanupNPCsInWorld(World world) {
        if (world == null) return;
        
        int count = processIslandsInWorld(world, (islandUUID) -> {
            if (plugin.getFancyNpcManager() != null) {
                Npc npc = plugin.getFancyNpcManager().getIslandNpc(islandUUID);
                if (npc != null) {
                    try {
                        FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
                    } catch (Exception e) {
                        plugin.getLogger().warning("清理 NPC 失败: " + e.getMessage());
                    }
                }
            }
            // Citizens 的 NPC 会自动清理
        });
        
        if (count > 0) {
            debug("清理了 " + count + " 个 NPC 引用");
        }
    }
    
    /**
     * 处理指定世界中的所有岛屿
     * @param world 世界
     * @param action 对每个岛屿执行的操作
     * @return 处理的岛屿数量
     */
    private int processIslandsInWorld(World world, java.util.function.Consumer<UUID> action) {
        org.bukkit.configuration.ConfigurationSection section = 
            plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        
        if (section == null) return 0;
        
        int count = 0;
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                if (worldName != null && worldName.equals(world.getName())) {
                    action.accept(islandUUID);
                    count++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿 UUID: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("处理岛屿 " + key + " 时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return count;
    }
}
