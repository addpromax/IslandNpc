package com.magicbili.islandnpc.api;

import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.Location;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NPC提供者抽象基类
 * 提供通用的NPC管理功能实现
 * 
 * @author magicbili
 */
public abstract class AbstractNpcProvider implements NpcProvider {
    
    protected final IslandNpcPlugin plugin;
    protected final Map<UUID, Boolean> hiddenNpcs;
    protected HologramProvider hologramProvider;
    
    public AbstractNpcProvider(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        this.hiddenNpcs = new HashMap<>();
        this.hologramProvider = null; // 将在子类中初始化
    }
    
    /**
     * 设置全息图提供者
     */
    public void setHologramProvider(HologramProvider hologramProvider) {
        this.hologramProvider = hologramProvider;
    }
    
    /**
     * 获取全息图提供者
     */
    public HologramProvider getHologramProvider() {
        return hologramProvider;
    }
    
    /**
     * 创建NPC后的钩子 - 用于创建全息图等
     */
    protected void afterNpcCreated(UUID islandUUID, org.bukkit.Location location) {
        // 创建全息图
        if (hologramProvider != null && plugin.getIslandProvider() != null) {
            String ownerName = plugin.getIslandProvider().getIslandOwnerName(islandUUID);
            if (ownerName != null) {
                // 从配置读取全息图文本行
                java.util.List<String> lines = plugin.getConfigManager().getConfig().getStringList("npc.hologram.lines");
                if (lines.isEmpty()) {
                    lines = java.util.List.of("§b§l" + ownerName + " 的岛屿", "§7右键点击交互");
                }
                
                // 替换占位符
                java.util.List<String> finalLines = new java.util.ArrayList<>();
                for (String line : lines) {
                    finalLines.add(line.replace("{owner}", ownerName));
                }
                
                // 计算全息图位置
                double yOffset = plugin.getConfigManager().getConfig().getDouble("npc.hologram.position.y-offset", 2.8);
                org.bukkit.Location hologramLoc = location.clone().add(0, yOffset, 0);
                
                String id = "island_" + islandUUID.toString();
                boolean success = false;
                
                // 如果是FancyHolograms,应用背景设置
                if (hologramProvider instanceof com.magicbili.islandnpc.hologram.FancyHologramsProvider) {
                    success = ((com.magicbili.islandnpc.hologram.FancyHologramsProvider) hologramProvider)
                        .createIslandHologramWithBackground(id, hologramLoc, finalLines, plugin.getConfigManager().getConfig());
                } else {
                    success = hologramProvider.createHologram(id, hologramLoc, finalLines);
                }
                
                if (success) {
                    debug("已为岛屿 " + islandUUID + " 创建全息图");
                } else {
                    debug("创建全息图失败: " + islandUUID);
                }
            }
        }
    }
    
    /**
     * 删除NPC前的钩子 - 用于删除全息图等
     */
    protected void beforeNpcDeleted(UUID islandUUID) {
        // 删除全息图
        if (hologramProvider != null) {
            hologramProvider.deleteIslandHologram(islandUUID);
            debug("已删除岛屿 " + islandUUID + " 的全息图");
        }
    }
    
    /**
     * 输出debug日志（仅在debug模式启用时）
     */
    protected void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] [" + getProviderName() + "] " + message);
        }
    }
    
    /**
     * 计算NPC生成位置（包含偏移量和朝向）
     * @param islandCenter 岛屿中心位置
     * @return 计算后的生成位置
     */
    protected Location calculateSpawnLocation(Location islandCenter) {
        if (islandCenter == null) return null;

        double offsetX = plugin.getConfigManager().getSpawnOffsetX();
        double offsetY = plugin.getConfigManager().getSpawnOffsetY();
        double offsetZ = plugin.getConfigManager().getSpawnOffsetZ();
        float yaw = plugin.getConfigManager().getNpcYaw();
        float pitch = plugin.getConfigManager().getNpcPitch();

        Location spawnLoc = islandCenter.clone().add(offsetX, offsetY, offsetZ);
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(pitch);
        
        debug(String.format("计算生成位置: Yaw=%.1f, Pitch=%.1f", yaw, pitch));
        
        return spawnLoc;
    }
    
    @Override
    public boolean isNpcHidden(UUID islandUUID) {
        return hiddenNpcs.getOrDefault(islandUUID, false);
    }
    
    @Override
    public Listener getEventListener() {
        // 默认不需要额外的事件监听器
        return null;
    }
    
    @Override
    public void cleanup() {
        // 默认清理：保存数据
        saveAllNpcData();
    }
    
    /**
     * 从配置加载NPC隐藏状态
     */
    protected void loadHiddenStates() {
        org.bukkit.configuration.ConfigurationSection section = 
            plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) {
            return;
        }
        
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                boolean hidden = section.getBoolean(key + ".hidden", false);
                hiddenNpcs.put(islandUUID, hidden);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿UUID: " + key);
            }
        }
    }
}
