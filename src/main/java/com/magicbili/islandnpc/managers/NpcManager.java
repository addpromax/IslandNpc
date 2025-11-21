package com.magicbili.islandnpc.managers;

import com.bgsoftware.superiorskyblock.api.island.Island;
import com.magicbili.islandnpc.IslandNpcPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcManager {

    private final IslandNpcPlugin plugin;
    private final NPCRegistry npcRegistry;
    private final Map<UUID, Integer> islandNpcs;
    private final Map<UUID, Boolean> hiddenNpcs;

    public NpcManager(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
        this.islandNpcs = new HashMap<>();
        this.hiddenNpcs = new HashMap<>();
        
        // 延迟加载NPC数据，等待Citizens和SlimeWorld完全加载完成
        // SlimeWorld的世界可能需要更长时间来加载
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            loadNpcData();
        }, 100L); // 5秒延迟，确保Citizens和SlimeWorld已加载完所有NPC
    }

    private void loadNpcData() {
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) {
            return;
        }

        int loaded = 0;
        
        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                boolean hidden = section.getBoolean(key + ".hidden", false);
                
                // 只加载隐藏状态，世界加载后会从配置重新创建NPC
                hiddenNpcs.put(islandUUID, hidden);
                loaded++;
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的岛屿UUID: " + key);
            }
        }
        
        if (loaded > 0) {
            plugin.getLogger().info("加载了 " + loaded + " 个岛屿的NPC配置");
        }
    }

    public void saveAllNpcData() {
        for (Map.Entry<UUID, Integer> entry : islandNpcs.entrySet()) {
            UUID islandUUID = entry.getKey();
            int npcId = entry.getValue();
            boolean hidden = hiddenNpcs.getOrDefault(islandUUID, false);
            
            NPC npc = npcRegistry.getById(npcId);
            if (npc == null) {
                continue;
            }

            String path = "npcs." + islandUUID.toString();
            
            // 保存位置信息
            Location loc = npc.getStoredLocation();
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
            String dialogId = npc.data().get("dialogId");
            if (dialogId != null) {
                plugin.getConfigManager().getNpcDataConfig().set(path + ".dialog-id", dialogId);
            }
        }
        plugin.getConfigManager().saveNpcData();
    }

    public NPC createIslandNpc(Island island) {
        if (island == null) return null;

        UUID islandUUID = island.getUniqueId();
        if (islandNpcs.containsKey(islandUUID)) {
            NPC existingNpc = npcRegistry.getById(islandNpcs.get(islandUUID));
            if (existingNpc != null) {
                return existingNpc;
            }
        }

        Location spawnLoc = calculateSpawnLocation(island);
        if (spawnLoc == null) return null;

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(plugin.getConfigManager().getNpcEntityType());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type, using VILLAGER as default");
            entityType = EntityType.VILLAGER;
        }

        NPC npc = npcRegistry.createNPC(entityType, "");
        
        if (entityType == EntityType.PLAYER) {
            String skin = plugin.getConfigManager().getNpcSkin();
            if (!skin.isEmpty()) {
                npc.getOrAddTrait(net.citizensnpcs.trait.SkinTrait.class).setSkinName(skin);
            }
        }

        // spawn NPC
        npc.spawn(spawnLoc);
        
        // 设置持久化数据
        npc.data().setPersistent("islandUUID", islandUUID.toString());
        npc.data().setPersistent("dialogId", plugin.getConfigManager().getDialogId());

        setupHologram(npc);

        // 保存我们的映射关系和位置数据
        islandNpcs.put(islandUUID, npc.getId());
        hiddenNpcs.put(islandUUID, false);
        saveAllNpcData();

        plugin.getLogger().info("Created NPC #" + npc.getId() + " for island: " + islandUUID);
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

        return islandSpawn.clone().add(offsetX, offsetY, offsetZ);
    }

    public NPC getIslandNpc(UUID islandUUID) {
        if (!islandNpcs.containsKey(islandUUID)) return null;
        return npcRegistry.getById(islandNpcs.get(islandUUID));
    }
    
    public java.util.Set<UUID> getAllIslandUUIDs() {
        return islandNpcs.keySet();
    }
    
    /**
     * 从保存的数据重新创建NPC（用于世界加载后）
     */
    public void recreateNpcForIsland(UUID islandUUID) {
        // 检查是否已经有NPC
        if (islandNpcs.containsKey(islandUUID)) {
            NPC existingNpc = npcRegistry.getById(islandNpcs.get(islandUUID));
            if (existingNpc != null && existingNpc.isSpawned()) {
                return; // NPC已存在且已spawn
            }
        }
        
        // 从配置读取保存的数据
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs." + islandUUID.toString());
        if (section == null) {
            return;
        }
        
        // 读取位置
        String worldName = section.getString("location.world");
        double x = section.getDouble("location.x");
        double y = section.getDouble("location.y");
        double z = section.getDouble("location.z");
        float yaw = (float) section.getDouble("location.yaw");
        float pitch = (float) section.getDouble("location.pitch");
        
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        
        Location location = new Location(world, x, y, z, yaw, pitch);
        boolean hidden = section.getBoolean("hidden", false);
        String dialogId = section.getString("dialog-id");
        
        // 创建NPC
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(plugin.getConfigManager().getNpcEntityType());
        } catch (IllegalArgumentException e) {
            entityType = EntityType.VILLAGER;
        }
        
        NPC npc = npcRegistry.createNPC(entityType, "");
        
        // 设置皮肤
        if (entityType == EntityType.PLAYER) {
            String skin = plugin.getConfigManager().getNpcSkin();
            if (!skin.isEmpty()) {
                npc.getOrAddTrait(net.citizensnpcs.trait.SkinTrait.class).setSkinName(skin);
            }
        }
        
        // Spawn NPC
        if (!hidden) {
            npc.spawn(location);
        }
        
        // 设置数据
        npc.data().setPersistent("islandUUID", islandUUID.toString());
        if (dialogId != null) {
            npc.data().setPersistent("dialogId", dialogId);
        } else {
            npc.data().setPersistent("dialogId", plugin.getConfigManager().getDialogId());
        }
        
        // 设置hologram
        setupHologram(npc);
        
        // 保存映射
        islandNpcs.put(islandUUID, npc.getId());
        hiddenNpcs.put(islandUUID, hidden);
    }

    public void hideNpc(UUID islandUUID) {
        NPC npc = getIslandNpc(islandUUID);
        if (npc != null && npc.isSpawned()) {
            npc.despawn();
            hiddenNpcs.put(islandUUID, true);
            saveAllNpcData();
        }
    }

    public void showNpc(UUID islandUUID) {
        NPC npc = getIslandNpc(islandUUID);
        if (npc != null && !npc.isSpawned()) {
            Location loc = npc.getStoredLocation();
            if (loc != null) {
                npc.spawn(loc);
                hiddenNpcs.put(islandUUID, false);
                saveAllNpcData();
            }
        }
    }

    public boolean isNpcHidden(UUID islandUUID) {
        return hiddenNpcs.getOrDefault(islandUUID, false);
    }

    public void moveNpc(UUID islandUUID, Location newLocation) {
        NPC npc = getIslandNpc(islandUUID);
        if (npc != null) {
            boolean wasSpawned = npc.isSpawned();
            if (wasSpawned) {
                npc.despawn();
            }
            npc.spawn(newLocation);
            if (!wasSpawned) {
                npc.despawn();
            }
            // 保存NPC位置数据
            saveAllNpcData();
        }
    }

    public void deleteNpc(UUID islandUUID) {
        NPC npc = getIslandNpc(islandUUID);
        if (npc != null) {
            npc.destroy();
            islandNpcs.remove(islandUUID);
            hiddenNpcs.remove(islandUUID);
            saveAllNpcData();
        }
    }

    public void reloadAllNpcs() {
        for (Map.Entry<UUID, Integer> entry : islandNpcs.entrySet()) {
            UUID islandUUID = entry.getKey();
            NPC npc = npcRegistry.getById(entry.getValue());
            if (npc != null && npc.isSpawned()) {
                setupHologram(npc);
                npc.data().setPersistent("dialogId", plugin.getConfigManager().getDialogId());
            }
        }
        // 保存更新的数据
        saveAllNpcData();
        plugin.getLogger().info("Reloaded all NPCs with updated holograms and dialog IDs");
    }

    public UUID getIslandUUIDFromNpc(NPC npc) {
        if (npc == null) return null;
        String uuidStr = npc.data().get("islandUUID");
        if (uuidStr == null) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void setupHologram(NPC npc) {
        if (!plugin.getConfigManager().isHologramEnabled()) {
            return;
        }

        try {
            net.citizensnpcs.trait.HologramTrait hologramTrait = npc.getOrAddTrait(net.citizensnpcs.trait.HologramTrait.class);
            
            hologramTrait.clear();
            
            java.util.List<String> lines = plugin.getConfigManager().getHologramLines();
            if (lines != null && !lines.isEmpty()) {
                for (String line : lines) {
                    String coloredLine = ChatColor.translateAlternateColorCodes('&', line);
                    hologramTrait.addLine(coloredLine);
                }
            }
            
            double lineHeight = plugin.getConfigManager().getHologramLineHeight();
            if (lineHeight > 0) {
                hologramTrait.setLineHeight(lineHeight);
            }
            
            int viewRange = plugin.getConfigManager().getHologramViewRange();
            if (viewRange > 0) {
                hologramTrait.setViewRange(viewRange);
            }
            
            plugin.getLogger().info("Hologram setup complete for NPC: " + npc.getId());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to setup hologram for NPC: " + e.getMessage());
        }
    }
}
