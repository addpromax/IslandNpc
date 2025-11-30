package com.magicbili.islandnpc.npc;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.AbstractNpcProvider;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * FancyNpcs NPC提供者实现
 * 
 * @author magicbili
 */
public class FancyNpcProvider extends AbstractNpcProvider {
    
    private final Map<UUID, String> islandNpcs;
    private final Set<UUID> pendingSaves = new HashSet<>();
    private org.bukkit.scheduler.BukkitTask saveTask = null;
    
    public FancyNpcProvider(IslandNpcPlugin plugin) {
        super(plugin);
        this.islandNpcs = new HashMap<>();
        
        // 延迟加载NPC数据
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, this::loadNpcData, 100L);
    }
    
    @Override
    public String getProviderName() {
        return "FancyNpcs";
    }
    
    @Override
    public boolean createNpc(UUID islandUUID, Location location) {
        if (location == null) {
            debug("创建NPC失败: 位置为null");
            return false;
        }
        
        if (islandNpcs.containsKey(islandUUID)) {
            Npc existingNpc = FancyNpcsPlugin.get().getNpcManager().getNpcById(islandNpcs.get(islandUUID));
            if (existingNpc != null) {
                debug("NPC已存在: " + islandUUID);
                return true;
            }
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(plugin.getConfigManager().getNpcEntityType());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的实体类型，使用默认值 VILLAGER");
            entityType = EntityType.VILLAGER;
        }

        // 创建NPC
        String npcName = "island_npc_" + islandUUID.toString().substring(0, 8);
        NpcData npcData = new NpcData(npcName, null, location);
        npcData.setType(entityType);
        npcData.setDisplayName("");
        
        if (entityType == EntityType.PLAYER) {
            String skin = plugin.getConfigManager().getNpcSkin();
            if (!skin.isEmpty()) {
                npcData.setSkin(skin);
            }
        }

        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);
        npc.create();
        npc.setSaveToFile(false);
        FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
        npc.spawnForAll();

        // 保存映射关系
        islandNpcs.put(islandUUID, npc.getData().getId());
        hiddenNpcs.put(islandUUID, false);
        saveSingleNpcData(islandUUID);

        plugin.getLogger().info("创建 FancyNPC " + npc.getData().getId() + " 用于岛屿: " + islandUUID);
        
        // 调用创建后钩子
        afterNpcCreated(islandUUID, location);
        
        return true;
    }
    
    @Override
    public boolean deleteNpc(UUID islandUUID) {
        if (!islandNpcs.containsKey(islandUUID)) {
            debug("删除NPC失败: 岛屿没有NPC - " + islandUUID);
            return false;
        }
        
        // 调用删除前钩子
        beforeNpcDeleted(islandUUID);
        
        Npc npc = getNpc(islandUUID);
        if (npc != null) {
            npc.removeForAll();
            FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
            debug("已销毁 NPC " + npc.getData().getId());
        }
        
        islandNpcs.remove(islandUUID);
        hiddenNpcs.remove(islandUUID);
        
        // 从配置中删除
        plugin.getConfigManager().getNpcDataConfig().set("npcs." + islandUUID.toString(), null);
        plugin.getConfigManager().saveNpcData();
        
        plugin.getLogger().info("删除岛屿NPC: " + islandUUID);
        return true;
    }
    
    @Override
    public boolean hideNpc(UUID islandUUID) {
        Npc npc = getNpc(islandUUID);
        if (npc == null) {
            debug("隐藏NPC失败: NPC不存在 - " + islandUUID);
            return false;
        }
        
        npc.removeForAll();
        hiddenNpcs.put(islandUUID, true);
        saveSingleNpcData(islandUUID);
        
        debug("已隐藏 NPC: " + islandUUID);
        return true;
    }
    
    @Override
    public boolean showNpc(UUID islandUUID) {
        Npc npc = getNpc(islandUUID);
        if (npc == null) {
            debug("显示NPC失败: NPC不存在 - " + islandUUID);
            return false;
        }
        
        npc.spawnForAll();
        hiddenNpcs.put(islandUUID, false);
        saveSingleNpcData(islandUUID);
        
        debug("已显示 NPC: " + islandUUID);
        return true;
    }
    
    @Override
    public boolean moveNpc(UUID islandUUID, Location newLocation) {
        Npc npc = getNpc(islandUUID);
        if (npc == null) {
            debug("移动NPC失败: NPC不存在 - " + islandUUID);
            return false;
        }
        
        boolean wasHidden = isNpcHidden(islandUUID);
        npc.removeForAll();
        npc.getData().setLocation(newLocation);
        npc.updateForAll();
        
        if (!wasHidden) {
            npc.spawnForAll();
        }
        
        saveSingleNpcData(islandUUID);
        debug("已移动 NPC: " + islandUUID);
        return true;
    }
    
    @Override
    public boolean hasNpc(UUID islandUUID) {
        return islandNpcs.containsKey(islandUUID) && getNpc(islandUUID) != null;
    }
    
    @Override
    public Set<UUID> getAllIslandUUIDs() {
        return islandNpcs.keySet();
    }
    
    @Override
    public boolean recreateNpc(UUID islandUUID) {
        // 检查是否已经有NPC
        if (islandNpcs.containsKey(islandUUID)) {
            Npc existingNpc = FancyNpcsPlugin.get().getNpcManager().getNpcById(islandNpcs.get(islandUUID));
            if (existingNpc != null && existingNpc.getEntityId() > 0) {
                debug("NPC已存在且已生成，跳过重新创建: " + islandUUID);
                return true;
            }
        }
        
        // 从配置读取保存的数据
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig()
            .getConfigurationSection("npcs." + islandUUID.toString());
        if (section == null) {
            debug("未找到岛屿的NPC配置数据: " + islandUUID);
            return false;
        }
        
        // 读取位置
        String worldName = section.getString("location.world");
        double x = section.getDouble("location.x");
        double y = section.getDouble("location.y");
        double z = section.getDouble("location.z");
        float yaw = plugin.getConfigManager().getNpcYaw();
        float pitch = plugin.getConfigManager().getNpcPitch();
        
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            debug("世界不存在: " + worldName);
            return false;
        }
        
        Location location = new Location(world, x, y, z, yaw, pitch);
        boolean hidden = section.getBoolean("hidden", false);
        
        // 创建NPC
        boolean success = createNpc(islandUUID, location);
        if (success && hidden) {
            hideNpc(islandUUID);
        }
        
        debug("重新创建NPC: " + islandUUID + ", 成功=" + success);
        return success;
    }
    
    @Override
    public void reloadAllNpcs() {
        debug("重新加载所有NPC...");
        for (UUID islandUUID : islandNpcs.keySet()) {
            Npc npc = getNpc(islandUUID);
            if (npc != null) {
                boolean wasHidden = isNpcHidden(islandUUID);
                npc.removeForAll();
                npc.updateForAll();
                if (!wasHidden) {
                    npc.spawnForAll();
                }
            }
        }
        debug("完成重新加载");
    }
    
    /**
     * 保存单个 NPC 数据（性能优化：只保存一个 NPC）
     */
    private void saveSingleNpcData(UUID islandUUID) {
        if (!islandNpcs.containsKey(islandUUID)) {
            return;
        }
        
        String npcId = islandNpcs.get(islandUUID);
        boolean hidden = hiddenNpcs.getOrDefault(islandUUID, false);
        
        Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(npcId);
        if (npc == null) {
            return;
        }

        String path = "npcs." + islandUUID.toString();
        
        // 保存NPC ID
        plugin.getConfigManager().getNpcDataConfig().set(path + ".npc-id", npcId);
        
        // 保存位置信息
        Location loc = npc.getData().getLocation();
        if (loc != null) {
            plugin.getConfigManager().getNpcDataConfig().set(path + ".location.world", 
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
            plugin.getConfigManager().getNpcDataConfig().set(path + ".location.x", loc.getX());
            plugin.getConfigManager().getNpcDataConfig().set(path + ".location.y", loc.getY());
            plugin.getConfigManager().getNpcDataConfig().set(path + ".location.z", loc.getZ());
            plugin.getConfigManager().getNpcDataConfig().set(path + ".location.yaw", loc.getYaw());
            plugin.getConfigManager().getNpcDataConfig().set(path + ".location.pitch", loc.getPitch());
            
            // 保存是否为 SlimeWorld
            boolean isSlimeWorld = loc.getWorld() != null && 
                com.magicbili.islandnpc.utils.WorldUtils.isSlimeWorld(loc.getWorld());
            plugin.getConfigManager().getNpcDataConfig().set(path + ".is_slimeworld", isSlimeWorld);
        }
        
        // 保存隐藏状态
        plugin.getConfigManager().getNpcDataConfig().set(path + ".hidden", hidden);
        
        // 异步保存到磁盘（防抖：合并多次保存请求）
        scheduleSave();
    }
    
    /**
     * 安排延迟保存任务（防抖机制）
     * 多次调用会合并为一次保存，避免频繁 I/O
     */
    private void scheduleSave() {
        if (saveTask != null && !saveTask.isCancelled()) {
            // 已有保存任务，取消并重新安排
            saveTask.cancel();
        }
        
        // 延迟 20 ticks (1秒) 后保存，期间的多次保存会被合并
        saveTask = org.bukkit.Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            plugin.getConfigManager().saveNpcData();
            debug("[防抖保存] 已保存 NPC 数据到磁盘");
        }, 20L);
    }
    
    @Override
    public void saveAllNpcData() {
        for (Map.Entry<UUID, String> entry : islandNpcs.entrySet()) {
            UUID islandUUID = entry.getKey();
            String npcId = entry.getValue();
            boolean hidden = hiddenNpcs.getOrDefault(islandUUID, false);
            
            Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(npcId);
            if (npc == null) {
                continue;
            }

            String path = "npcs." + islandUUID.toString();
            
            // 保存NPC ID
            plugin.getConfigManager().getNpcDataConfig().set(path + ".npc-id", npcId);
            
            // 保存位置信息
            Location loc = npc.getData().getLocation();
            if (loc != null) {
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.world", 
                    loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.x", loc.getX());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.y", loc.getY());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.z", loc.getZ());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.yaw", loc.getYaw());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.pitch", loc.getPitch());
                
                // 保存是否为 SlimeWorld
                boolean isSlimeWorld = loc.getWorld() != null && 
                    com.magicbili.islandnpc.utils.WorldUtils.isSlimeWorld(loc.getWorld());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".is_slimeworld", isSlimeWorld);
            }
            
            // 保存隐藏状态
            plugin.getConfigManager().getNpcDataConfig().set(path + ".hidden", hidden);
        }
        plugin.getConfigManager().saveNpcData();
    }
    
    @Override
    public void cleanup() {
        debug("清理 FancyNpcs 提供者...");
        saveAllNpcData();
        debug("清理完成");
    }
    
    /**
     * 获取岛屿的NPC
     */
    private Npc getNpc(UUID islandUUID) {
        if (!islandNpcs.containsKey(islandUUID)) {
            return null;
        }
        return FancyNpcsPlugin.get().getNpcManager().getNpcById(islandNpcs.get(islandUUID));
    }
    
    /**
     * 加载NPC数据
     */
    private void loadNpcData() {
        debug("开始加载NPC数据...");
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig()
            .getConfigurationSection("npcs");
        if (section == null) {
            debug("没有找到NPC数据配置节点");
            return;
        }

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                String npcId = section.getString(key + ".npc-id");
                boolean hidden = section.getBoolean(key + ".hidden", false);
                
                if (npcId != null) {
                    islandNpcs.put(islandUUID, npcId);
                }
                hiddenNpcs.put(islandUUID, hidden);
                loaded++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿UUID: " + key);
            }
        }
        
        if (loaded > 0) {
            plugin.getLogger().info("加载了 " + loaded + " 个岛屿的NPC配置");
        }
        debug("NPC数据加载完成");
    }
}
