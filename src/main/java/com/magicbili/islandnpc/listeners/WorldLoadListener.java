package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.magicbili.islandnpc.IslandNpcPlugin;
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
            debug("[世界加载] 已由 SlimeWorld 事件处理，跳过");
            return;
        }
        
        // 延迟处理，确保世界完全加载（20 ticks = 1秒）
        scheduleNpcRespawn(worldName, 20L, false);
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
        
        // 延迟处理，SlimeWorld 加载很快，只需短暂延迟（10 ticks = 0.5秒）
        scheduleNpcRespawn(worldName, 10L, true);
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
     * @param worldName 世界名称
     * @param delayTicks 延迟时间（ticks）
     * @param isSlimeWorld 是否为 SlimeWorld
     */
    private void scheduleNpcRespawn(String worldName, long delayTicks, boolean isSlimeWorld) {
        debug("安排延迟任务: " + delayTicks + " ticks 后处理世界 " + worldName + 
              " (SlimeWorld: " + isSlimeWorld + ")");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    debug("延迟处理: 开始重新创建 NPC - " + worldName);
                    respawnNPCsInWorld(world, isSlimeWorld);
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
     * @param world 世界
     * @param isSlimeWorld 是否为 SlimeWorld（用于过滤）
     */
    private void respawnNPCsInWorld(World world, boolean isSlimeWorld) {
        if (world == null) return;
        
        debug("开始处理世界 " + world.getName() + " 的 NPC (SlimeWorld: " + isSlimeWorld + ")");
        
        int count = processIslandsInWorld(world, isSlimeWorld, (islandUUID) -> {
            if (plugin.getNpcProvider() != null) {
                debug("尝试重新创建 NPC: " + islandUUID);
                boolean success = plugin.getNpcProvider().recreateNpc(islandUUID);
                debug("重新创建 NPC " + islandUUID + " 结果: " + success);
            }
        });
        
        if (count > 0) {
            debug("重新创建了 " + count + " 个 NPC");
        } else {
            debug("世界 " + world.getName() + " 中没有找到匹配的岛屿配置 (期望 SlimeWorld: " + isSlimeWorld + ")");
        }
    }
    
    /**
     * 清理指定世界中的 NPC 引用
     */
    private void cleanupNPCsInWorld(World world) {
        if (world == null) return;
        
        // 清理时不区分世界类型，处理所有岛屿
        org.bukkit.configuration.ConfigurationSection section = 
            plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        
        if (section == null) return;
        
        int count = 0;
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                if (worldName != null && worldName.equals(world.getName())) {
                    // NPC提供者会在世界重新加载时自动重建NPC
                    // 这里不需要特殊处理
                    count++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿 UUID: " + key);
            }
        }
        
        if (count > 0) {
            debug("处理了 " + count + " 个岛屿的 NPC");
        }
    }
    
    /**
     * 处理指定世界中的所有岛屿
     * @param world 世界
     * @param filterSlimeWorld 是否只处理 SlimeWorld 岛屿（true=只处理SlimeWorld，false=只处理普通世界）
     * @param action 对每个岛屿执行的操作
     * @return 处理的岛屿数量
     */
    private int processIslandsInWorld(World world, boolean filterSlimeWorld, java.util.function.Consumer<UUID> action) {
        org.bukkit.configuration.ConfigurationSection section = 
            plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        
        if (section == null) return 0;
        
        int count = 0;
        int skipped = 0;
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                if (worldName != null && worldName.equals(world.getName())) {
                    // 性能优化：使用 is_slimeworld 标记过滤
                    // 注意：如果配置中没有 is_slimeworld 字段（旧数据），需要动态检测
                    boolean isSlimeWorld;
                    if (section.contains(key + ".is_slimeworld")) {
                        isSlimeWorld = section.getBoolean(key + ".is_slimeworld");
                    } else {
                        // 旧数据没有标记，动态检测世界类型
                        isSlimeWorld = com.magicbili.islandnpc.utils.WorldUtils.isSlimeWorld(world);
                        debug("岛屿 " + key + " 缺少 is_slimeworld 标记，动态检测结果: " + isSlimeWorld);
                    }
                    
                    // 只处理匹配的世界类型
                    if (isSlimeWorld == filterSlimeWorld) {
                        debug("处理岛屿: " + islandUUID + " (SlimeWorld: " + isSlimeWorld + ")");
                        action.accept(islandUUID);
                        count++;
                    } else {
                        skipped++;
                        debug("跳过岛屿: " + islandUUID + " (期望 SlimeWorld: " + filterSlimeWorld + ", 实际: " + isSlimeWorld + ")");
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿 UUID: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("处理岛屿 " + key + " 时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (skipped > 0) {
            debug("跳过了 " + skipped + " 个不匹配的岛屿类型");
        }
        
        return count;
    }
}
