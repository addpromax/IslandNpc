package com.magicbili.islandnpc.managers;

import com.bgsoftware.superiorskyblock.api.island.Island;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import com.magicbili.islandnpc.IslandNpcPlugin;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.actions.NpcAction;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FancyNpcManager {

    private final IslandNpcPlugin plugin;
    private final Map<UUID, String> islandNpcs;
    private final Map<UUID, Boolean> hiddenNpcs;
    private final Map<UUID, Hologram> hologramMap; // 存储每个岛屿的FancyHolograms全息图

    public FancyNpcManager(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        this.islandNpcs = new HashMap<>();
        this.hiddenNpcs = new HashMap<>();
        this.hologramMap = new HashMap<>();
        
        // 延迟加载NPC数据，等待FancyNpcs和FancyHolograms完全加载完成
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            loadNpcData();
        }, 100L);
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

    private void loadNpcData() {
        debug("开始加载 NPC 数据...");
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) {
            debug("没有找到 NPC 数据配置节点");
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
            debug("加载了 " + loaded + " 个岛屿的 NPC 配置");
        } else {
            debug("没有加载到任何 NPC 配置");
        }
    }

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
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.world", loc.getWorld() != null ? loc.getWorld().getName() : "unknown");
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.x", loc.getX());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.y", loc.getY());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.z", loc.getZ());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.yaw", loc.getYaw());
                plugin.getConfigManager().getNpcDataConfig().set(path + ".location.pitch", loc.getPitch());
            }
            
            // 保存隐藏状态
            plugin.getConfigManager().getNpcDataConfig().set(path + ".hidden", hidden);
            
            // 保存对话框ID
            String dialogId = getDialogIdFromNpc(npc);
            if (dialogId != null) {
                plugin.getConfigManager().getNpcDataConfig().set(path + ".dialog-id", dialogId);
            }
        }
        plugin.getConfigManager().saveNpcData();
    }

    public Npc createIslandNpc(Island island) {
        if (island == null) {
            debug("createIslandNpc: island 参数为 null");
            return null;
        }

        UUID islandUUID = island.getUniqueId();
        debug("开始为岛屿创建 NPC: " + islandUUID);
        
        if (islandNpcs.containsKey(islandUUID)) {
            debug("岛屿已有 NPC 记录，检查是否存在: " + islandNpcs.get(islandUUID));
            Npc existingNpc = FancyNpcsPlugin.get().getNpcManager().getNpcById(islandNpcs.get(islandUUID));
            if (existingNpc != null) {
                debug("NPC 已存在，返回现有 NPC");
                return existingNpc;
            } else {
                debug("NPC 记录存在但实例不存在，将重新创建");
            }
        }

        Location spawnLoc = calculateSpawnLocation(island);
        if (spawnLoc == null) {
            debug("无法计算 NPC 生成位置！");
            return null;
        }
        debug(String.format("NPC 生成位置: 世界=%s, X=%.2f, Y=%.2f, Z=%.2f", 
            spawnLoc.getWorld() != null ? spawnLoc.getWorld().getName() : "null",
            spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));

        EntityType entityType;
        try {
            String configType = plugin.getConfigManager().getNpcEntityType();
            debug("配置的实体类型: " + configType);
            entityType = EntityType.valueOf(configType);
        } catch (IllegalArgumentException e) {
            debug("无效的实体类型，使用默认值 VILLAGER: " + e.getMessage());
            entityType = EntityType.VILLAGER;
        }
        debug("使用实体类型: " + entityType);

        // 创建NPC数据
        String npcName = "island_npc_" + islandUUID.toString().substring(0, 8);
        debug("创建 NpcData，名称: " + npcName);
        
        NpcData npcData = new NpcData(npcName, plugin.getServer().getConsoleSender() != null ? 
            UUID.fromString("00000000-0000-0000-0000-000000000000") : null, spawnLoc);
        
        npcData.setType(entityType);
        npcData.setDisplayName("");
        debug("NpcData ID: " + npcData.getId());
        
        // 如果是玩家类型，设置皮肤
        if (entityType == EntityType.PLAYER) {
            String skin = plugin.getConfigManager().getNpcSkin();
            debug("玩家类型 NPC，皮肤配置: " + (skin.isEmpty() ? "未设置" : skin));
            if (!skin.isEmpty()) {
                npcData.setSkin(skin);
                debug("已设置皮肤: " + skin);
            }
        }

        // 不再使用 NPC 的 displayName 作为全息图，保持为空
        // 全息图将在 NPC 创建后使用独立的 TextDisplay 实体

        // 不再添加对话框动作，只使用 TypeWriter 交互

        // 创建NPC（按照正确的顺序）
        debug("开始创建 NPC 实例...");
        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);
        debug("NPC 实例已创建，调用 create()...");
        
        try {
            npc.create();
            debug("NPC create() 成功");
            
            // 关键：设置为不保存到FancyNpcs配置文件，由我们自己管理持久化
            npc.setSaveToFile(false);
            debug("已设置 NPC 为临时对象（不保存到FancyNpcs配置）");
        } catch (Exception e) {
            debug("NPC create() 失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        
        // 注册NPC（必须在spawnForAll之前）
        debug("注册 NPC 到 FancyNpcs 管理器...");
        try {
            FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
            debug("NPC 注册成功");
        } catch (Exception e) {
            debug("NPC 注册失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        
        // 生成NPC给所有玩家
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
        debug("为所有玩家生成 NPC（在线玩家数: " + onlinePlayers + ")...");
        try {
            npc.spawnForAll();
            debug("NPC 已为所有玩家生成");
            debug("NPC 属性 - spawnEntity: " + npc.getData().isSpawnEntity() + 
                ", showInTab: " + npc.getData().isShowInTab() + 
                ", collidable: " + npc.getData().isCollidable());
        } catch (Exception e) {
            debug("NPC spawnForAll() 失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 保存映射关系
        debug("保存 NPC 映射关系...");
        islandNpcs.put(islandUUID, npcData.getId());
        hiddenNpcs.put(islandUUID, false);
        saveAllNpcData();
        debug("NPC 数据已保存");

        plugin.getLogger().info("[SUCCESS] 成功创建 FancyNPC " + npcData.getId() + " 用于岛屿: " + islandUUID);
        debug("NPC EntityID: " + npc.getEntityId());
        debug("NPC SaveToFile: " + npc.isSaveToFile());
        debug("NPC 是否已生成: " + (npc.getEntityId() > 0));
        
        // 创建独立的全息图 TextDisplay 实体
        createHologram(islandUUID, spawnLoc);
        
        return npc;
    }

    private Location calculateSpawnLocation(Island island) {
        // 使用NORMAL维度获取岛屿中心位置
        com.bgsoftware.superiorskyblock.api.world.Dimension normalDimension = 
            com.bgsoftware.superiorskyblock.api.world.Dimension.getByName("NORMAL");
        Location islandSpawn = island.getCenter(normalDimension);
        if (islandSpawn == null) return null;

        double offsetX = plugin.getConfigManager().getSpawnOffsetX();
        double offsetY = plugin.getConfigManager().getSpawnOffsetY();
        double offsetZ = plugin.getConfigManager().getSpawnOffsetZ();
        float yaw = plugin.getConfigManager().getNpcYaw();
        float pitch = plugin.getConfigManager().getNpcPitch();

        Location spawnLoc = islandSpawn.clone().add(offsetX, offsetY, offsetZ);
        spawnLoc.setYaw(yaw);
        spawnLoc.setPitch(pitch);
        
        debug(String.format("NPC朝向: Yaw=%.1f, Pitch=%.1f", yaw, pitch));
        
        return spawnLoc;
    }

    public Npc getIslandNpc(UUID islandUUID) {
        if (!islandNpcs.containsKey(islandUUID)) return null;
        return FancyNpcsPlugin.get().getNpcManager().getNpcById(islandNpcs.get(islandUUID));
    }
    
    public Set<UUID> getAllIslandUUIDs() {
        return islandNpcs.keySet();
    }
    
    public void recreateNpcForIsland(UUID islandUUID) {
        debug("重新创建岛屿 NPC: " + islandUUID);
        
        // 检查是否已经有NPC
        if (islandNpcs.containsKey(islandUUID)) {
            Npc existingNpc = FancyNpcsPlugin.get().getNpcManager().getNpcById(islandNpcs.get(islandUUID));
            if (existingNpc != null) {
                debug("找到现有NPC对象，ID: " + existingNpc.getData().getId() + ", EntityID: " + existingNpc.getEntityId());
                
                // 检查NPC是否真正spawn（通过检查是否有玩家能看到它）
                boolean isSpawned = false;
                try {
                    // 检查NPC实体是否存在：尝试获取bukkit实体
                    if (existingNpc.getEntityId() > 0) {
                        org.bukkit.World world = existingNpc.getData().getLocation().getWorld();
                        if (world != null) {
                            // 尝试从世界获取实体
                            org.bukkit.entity.Entity entity = world.getEntities().stream()
                                .filter(e -> e.getEntityId() == existingNpc.getEntityId())
                                .findFirst()
                                .orElse(null);
                            isSpawned = (entity != null);
                            debug("实体检查结果: " + (isSpawned ? "实体存在" : "实体不存在"));
                        } else {
                            debug("NPC所在世界为null");
                        }
                    }
                } catch (Exception e) {
                    debug("检查NPC实体时出错: " + e.getMessage());
                }
                
                if (isSpawned) {
                    debug("NPC 已存在且已spawn，跳过重新创建");
                    return;
                } else {
                    debug("NPC 对象存在但实体未spawn，需要重新spawn");
                    // 清理旧的NPC注册，准备重新创建
                    try {
                        FancyNpcsPlugin.get().getNpcManager().removeNpc(existingNpc);
                        debug("已移除旧的NPC注册");
                    } catch (Exception e) {
                        debug("移除旧NPC失败: " + e.getMessage());
                    }
                    // 清理旧的全息图引用（世界卸载时TextDisplay实体已被移除）
                    removeHologram(islandUUID);
                }
            }
        }
        
        // 从配置读取保存的数据
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs." + islandUUID.toString());
        if (section == null) {
            debug("未找到岛屿的 NPC 配置数据");
            return;
        }
        
        // 读取位置
        String worldName = section.getString("location.world");
        double x = section.getDouble("location.x");
        double y = section.getDouble("location.y");
        double z = section.getDouble("location.z");
        
        // 使用配置文件中的朝向（而不是保存的朝向），这样可以统一管理
        float yaw = plugin.getConfigManager().getNpcYaw();
        float pitch = plugin.getConfigManager().getNpcPitch();
        
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            debug("世界不存在: " + worldName);
            return;
        }
        
        Location location = new Location(world, x, y, z, yaw, pitch);
        boolean hidden = section.getBoolean("hidden", false);
        
        debug(String.format("从配置读取位置: 世界=%s, X=%.2f, Y=%.2f, Z=%.2f, 朝向=Yaw:%.1f/Pitch:%.1f, 隐藏=%b", 
            worldName, x, y, z, yaw, pitch, hidden));
        
        // 创建NPC数据
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(plugin.getConfigManager().getNpcEntityType());
        } catch (IllegalArgumentException e) {
            entityType = EntityType.VILLAGER;
        }
        
        String npcName = "island_npc_" + islandUUID.toString().substring(0, 8);
        NpcData npcData = new NpcData(npcName, UUID.fromString("00000000-0000-0000-0000-000000000000"), location);
        npcData.setType(entityType);
        npcData.setDisplayName("");
        
        // 设置皮肤
        if (entityType == EntityType.PLAYER) {
            String skin = plugin.getConfigManager().getNpcSkin();
            if (!skin.isEmpty()) {
                npcData.setSkin(skin);
            }
        }
        
        // 不再使用 NPC 的 displayName 作为全息图
        // 全息图将在 NPC 创建后使用独立的 TextDisplay 实体
        
        // 不再添加对话框动作，只使用 TypeWriter 交互
        
        // 创建NPC（按照正确的顺序）
        debug("开始重新创建 NPC...");
        debug("NPC名称: " + npcName + ", 类型: " + entityType);
        Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(npcData);
        
        try {
            npc.create();
            debug("NPC create() 成功");
            
            // 关键：设置为不保存到FancyNpcs配置文件
            npc.setSaveToFile(false);
            debug("已设置 NPC 为临时对象（不保存到FancyNpcs配置）");
        } catch (Exception e) {
            debug("NPC create() 失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // 注册NPC（必须在spawnForAll之前）
        try {
            FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
            debug("NPC 注册成功");
        } catch (Exception e) {
            debug("NPC 注册失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        // 生成NPC
        if (!hidden) {
            int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
            debug("准备生成NPC（在线玩家数: " + onlinePlayers + "）");
            npc.spawnForAll();
            debug("NPC 已生成，EntityID: " + npc.getEntityId());
            debug("NPC DisplayName: '" + npc.getData().getDisplayName() + "'");
        } else {
            debug("NPC 处于隐藏状态，未生成");
        }
        
        // 保存映射
        islandNpcs.put(islandUUID, npcData.getId());
        hiddenNpcs.put(islandUUID, hidden);
        
        // 创建独立的全息图 TextDisplay 实体
        if (!hidden) {
            createHologram(islandUUID, location);
            debug("已创建全息图");
        }
        
        plugin.getLogger().info("[SUCCESS] 成功重新创建 NPC: " + npcData.getId() + 
            " (岛屿: " + islandUUID + ", 隐藏: " + hidden + 
            ", EntityID: " + npc.getEntityId() + ")");
    }

    public void moveNpc(UUID islandUUID, Location newLocation) {
        debug("移动岛屿 NPC: " + islandUUID);
        Npc npc = getIslandNpc(islandUUID);
        if (npc != null) {
            debug(String.format("新位置: X=%.2f, Y=%.2f, Z=%.2f",
                newLocation.getX(), newLocation.getY(), newLocation.getZ()));
            boolean wasHidden = isNpcHidden(islandUUID);
            npc.removeForAll();
            npc.getData().setLocation(newLocation);
            npc.updateForAll();
            if (!wasHidden) {
                npc.spawnForAll();
            }
            
            // 移动全息图到新位置
            moveHologram(islandUUID, newLocation);
            
            saveAllNpcData();
            debug("NPC 已移动");
        } else {
            debug("找不到要移动的 NPC");
        }
    }

    public void hideNpc(UUID islandUUID) {
        debug("隐藏岛屿 NPC: " + islandUUID);
        Npc npc = getIslandNpc(islandUUID);
        if (npc != null) {
            npc.removeForAll();
            hiddenNpcs.put(islandUUID, true);
            
            // 隐藏全息图
            hideHologram(islandUUID);
            
            saveAllNpcData();
            debug("NPC 已隐藏");
        } else {
            debug("找不到要隐藏的 NPC");
        }
    }

    public void showNpc(UUID islandUUID) {
        debug("显示岛屿 NPC: " + islandUUID);
        Npc npc = getIslandNpc(islandUUID);
        if (npc != null) {
            npc.spawnForAll();
            hiddenNpcs.put(islandUUID, false);
            
            // 显示全息图
            showHologram(islandUUID);
            
            saveAllNpcData();
            debug("NPC 已显示");
        } else {
            debug("找不到要显示的 NPC");
        }
    }

    public boolean isNpcHidden(UUID islandUUID) {
        return hiddenNpcs.getOrDefault(islandUUID, false);
    }

    public void deleteNpc(UUID islandUUID) {
        debug("删除岛屿 NPC: " + islandUUID);
        
        // 获取NPC实例
        Npc npc = getIslandNpc(islandUUID);
        String npcId = null;
        
        if (npc != null) {
            npcId = npc.getData().getId();
            debug("找到NPC实例，ID: " + npcId);
            
            try {
                // 从 FancyNpcs 管理器中移除
                FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
                debug("已从 FancyNpcs 管理器中移除 NPC");
            } catch (Exception e) {
                debug("从 FancyNpcs 管理器移除 NPC 失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            debug("NPC 实例不存在，但仍然清理配置数据");
        }
        
        // 从内存 Map 中移除
        String removedNpcId = islandNpcs.remove(islandUUID);
        Boolean wasHidden = hiddenNpcs.remove(islandUUID);
        
        if (removedNpcId != null) {
            debug("已从内存移除 NPC 映射: " + removedNpcId + ", 隐藏状态: " + wasHidden);
        }
        
        // 移除全息图
        removeHologram(islandUUID);
        
        // 关键：从配置文件中删除该岛屿的NPC数据
        String configPath = "npcs." + islandUUID.toString();
        if (plugin.getConfigManager().getNpcDataConfig().contains(configPath)) {
            plugin.getConfigManager().getNpcDataConfig().set(configPath, null);
            debug("已从配置文件中删除路径: " + configPath);
        } else {
            debug("配置文件中未找到路径: " + configPath);
        }
        
        // 保存更改
        plugin.getConfigManager().saveNpcData();
        
        plugin.getLogger().info("[SUCCESS] 已完成岛屿 NPC 删除: " + islandUUID + 
            (npcId != null ? " (NPC ID: " + npcId + ")" : ""));
    }

    public void reloadAllNpcs() {
        debug("开始重载所有 NPC...");
        int reloadedCount = 0;
        
        for (Map.Entry<UUID, String> entry : islandNpcs.entrySet()) {
            UUID islandUUID = entry.getKey();
            Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpcById(entry.getValue());
            if (npc != null) {
                debug("重载岛屿 " + islandUUID + " 的 NPC");
                
                // 移除旧的全息图
                removeHologram(islandUUID);
                
                // 清除所有 RIGHT_CLICK 动作（移除旧的对话框动作）
                npc.getData().setActions(ActionTrigger.RIGHT_CLICK, new ArrayList<>());
                debug("已清除所有 RIGHT_CLICK 动作（包括对话框动作）");
                
                // 刷新NPC
                npc.removeForAll();
                npc.updateForAll();
                
                boolean isHidden = isNpcHidden(islandUUID);
                if (!isHidden) {
                    npc.spawnForAll();
                    
                    // 重新创建全息图（使用新的配置）
                    Location npcLocation = npc.getData().getLocation();
                    createHologram(islandUUID, npcLocation);
                    debug("已重新创建全息图");
                } else {
                    debug("NPC 处于隐藏状态，跳过全息图创建");
                }
                
                reloadedCount++;
            }
        }
        
        saveAllNpcData();
        plugin.getLogger().info("[SUCCESS] 已重载 " + reloadedCount + " 个 NPC，应用了新的全息图配置");
    }

    public UUID getIslandUUIDFromNpc(Npc npc) {
        if (npc == null) return null;
        String npcId = npc.getData().getId();
        for (Map.Entry<UUID, String> entry : islandNpcs.entrySet()) {
            if (entry.getValue().equals(npcId)) {
                return entry.getKey();
            }
        }
        return null;
    }


    private String getDialogIdFromNpc(Npc npc) {
        List<NpcAction.NpcActionData> actions = npc.getData().getActions(ActionTrigger.RIGHT_CLICK);
        if (actions != null && !actions.isEmpty()) {
            for (NpcAction.NpcActionData actionData : actions) {
                if (actionData.action().getName().equalsIgnoreCase("open_dialog")) {
                    return actionData.value();
                }
            }
        }
        return null;
    }

    /**
     * 为岛屿创建FancyHolograms全息图
     */
    private void createHologram(UUID islandUUID, Location npcLocation) {
        if (!plugin.getConfigManager().isHologramEnabled()) {
            debug("全息图未启用，跳过创建");
            return;
        }
        
        // 检查FancyHolograms是否可用
        if (!FancyHologramsPlugin.isEnabled()) {
            plugin.getLogger().warning("FancyHolograms插件未加载，无法创建全息图");
            return;
        }
        
        // 验证位置和世界
        if (npcLocation == null) {
            debug("NPC位置为null，无法创建全息图");
            return;
        }
        
        if (npcLocation.getWorld() == null) {
            debug("NPC所在世界为null，无法创建全息图");
            return;
        }
        
        debug("NPC位置: 世界=" + npcLocation.getWorld().getName() + 
            ", X=" + npcLocation.getX() + 
            ", Y=" + npcLocation.getY() + 
            ", Z=" + npcLocation.getZ());

        List<String> lines = plugin.getConfigManager().getHologramLines();
        if (lines == null || lines.isEmpty()) {
            debug("全息图行数为空");
            return;
        }

        debug("开始创建FancyHolograms全息图，行数: " + lines.size());
        
        // 清除旧的全息图（如果存在）
        removeHologram(islandUUID);
        
        // 从配置文件读取位置设置
        double yOffset = plugin.getConfigManager().getHologramYOffset();
        
        // 读取背景设置
        boolean backgroundEnabled = plugin.getConfigManager().isHologramBackgroundEnabled();
        int backgroundColor = plugin.getConfigManager().getHologramBackgroundColor();
        
        debug(String.format("全息图位置: Y偏移=%.2f", yOffset));
        debug(String.format("背景设置: 启用=%b, 颜色=0x%08X", backgroundEnabled, backgroundColor));
        
        try {
            // 计算全息图位置（在NPC上方）
            Location hologramLoc = npcLocation.clone().add(0, yOffset, 0);
            
            // 创建全息图名称（使用岛屿UUID确保唯一性）
            String hologramName = "island_hologram_" + islandUUID.toString();
            
            // 使用 V2 API 创建 TextHologramData
            TextHologramData hologramData = new TextHologramData(hologramName, hologramLoc);
            
            // 设置文本行（支持颜色代码）
            List<String> coloredLines = new ArrayList<>();
            for (String line : lines) {
                coloredLines.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            hologramData.setText(coloredLines);
            
            // 设置文本对齐方式
            hologramData.setTextAlignment(TextDisplay.TextAlignment.CENTER);
            
            // 设置文本阴影
            hologramData.setTextShadow(true);
            
            // 设置透视（穿透方块可见）
            hologramData.setSeeThrough(true);
            
            // 设置背景颜色
            if (!backgroundEnabled) {
                // 不启用背景，设置为完全透明
                hologramData.setBackground(Hologram.TRANSPARENT);
                debug("设置全息图背景为透明");
            } else {
                // 启用背景，使用配置的颜色
                Color bgColor = Color.fromARGB(backgroundColor);
                hologramData.setBackground(bgColor);
                debug(String.format("设置全息图背景颜色: ARGB=0x%08X", backgroundColor));
            }
            
            // 设置为非持久化（不保存到FancyHolograms配置文件）
            hologramData.setPersistent(false);
            
            // 使用 HologramManager 创建全息图
            Hologram hologram = FancyHologramsPlugin.get()
                .getHologramManager()
                .create(hologramData);
            
            if (hologram != null) {
                // 添加到 HologramManager（重要！）
                FancyHologramsPlugin.get().getHologramManager().addHologram(hologram);
                
                // 为所有在线玩家更新显示状态
                for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                    hologram.updateShownStateFor(player);
                }
                
                // 保存到映射
                hologramMap.put(islandUUID, hologram);
                debug("成功创建并注册FancyHolograms全息图: " + hologramName);
                debug("全息图可见性: " + hologramData.getVisibility() + ", 可见距离: " + hologramData.getVisibilityDistance());
            } else {
                plugin.getLogger().warning("创建全息图失败: " + hologramName);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("创建FancyHolograms全息图失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 移除岛屿的全息图
     */
    private void removeHologram(UUID islandUUID) {
        Hologram hologram = hologramMap.remove(islandUUID);
        if (hologram != null) {
            debug("移除FancyHolograms全息图: " + hologram.getData().getName());
            try {
                // 从FancyHolograms管理器中移除
                if (FancyHologramsPlugin.isEnabled() && FancyHologramsPlugin.get() != null) {
                    hologram.deleteHologram();
                    FancyHologramsPlugin.get().getHologramManager().removeHologram(hologram);
                }
            } catch (NullPointerException e) {
                plugin.getLogger().warning("FancyHolograms未初始化，无法注销全息图: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().warning("移除全息图时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 清理所有全息图（插件禁用时调用）
     */
    public void cleanupAllHolograms() {
        debug("开始清理所有全息图，共 " + hologramMap.size() + " 个");
        
        // 使用副本避免并发修改异常
        for (UUID islandUUID : new ArrayList<>(hologramMap.keySet())) {
            removeHologram(islandUUID);
        }
        
        hologramMap.clear();
        debug("全息图清理完成");
    }

    /**
     * 隐藏岛屿的全息图
     */
    private void hideHologram(UUID islandUUID) {
        Hologram hologram = hologramMap.get(islandUUID);
        if (hologram != null) {
            debug("隐藏FancyHolograms全息图: " + hologram.getData().getName());
            try {
                // 对所有在线玩家隐藏全息图
                if (FancyHologramsPlugin.isEnabled()) {
                    hologram.hideHologram(plugin.getServer().getOnlinePlayers());
                }
            } catch (Exception e) {
                debug("隐藏全息图时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 显示岛屿的全息图
     */
    private void showHologram(UUID islandUUID) {
        Hologram hologram = hologramMap.get(islandUUID);
        if (hologram != null) {
            debug("显示FancyHolograms全息图: " + hologram.getData().getName());
            try {
                // 对所有在线玩家显示全息图
                if (FancyHologramsPlugin.isEnabled()) {
                    hologram.showHologram(plugin.getServer().getOnlinePlayers());
                    hologram.refreshHologram(plugin.getServer().getOnlinePlayers());
                }
            } catch (Exception e) {
                debug("显示全息图时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 移动岛屿的全息图到新位置
     */
    private void moveHologram(UUID islandUUID, Location newNpcLocation) {
        Hologram hologram = hologramMap.get(islandUUID);
        if (hologram != null) {
            debug("移动FancyHolograms全息图: " + hologram.getData().getName());
            try {
                // 计算新的全息图位置
                double yOffset = plugin.getConfigManager().getHologramYOffset();
                Location newHologramLoc = newNpcLocation.clone().add(0, yOffset, 0);
                
                // 更新全息图位置
                hologram.getData().setLocation(newHologramLoc);
                
                // 刷新全息图显示
                if (FancyHologramsPlugin.isEnabled()) {
                    hologram.queueUpdate();
                    hologram.refreshHologram(plugin.getServer().getOnlinePlayers());
                }
                
                debug("全息图已移动到新位置");
            } catch (Exception e) {
                debug("移动全息图时出错: " + e.getMessage());
            }
        }
    }
}
