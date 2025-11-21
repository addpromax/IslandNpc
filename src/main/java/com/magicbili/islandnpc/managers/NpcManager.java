package com.magicbili.islandnpc.managers;

import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
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
        loadNpcData();
    }

    private void loadNpcData() {
        ConfigurationSection section = plugin.getConfigManager().getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID islandUUID = UUID.fromString(key);
                int npcId = section.getInt(key + ".npc-id");
                boolean hidden = section.getBoolean(key + ".hidden", false);

                islandNpcs.put(islandUUID, npcId);
                hiddenNpcs.put(islandUUID, hidden);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in npcdata.yml: " + key);
            }
        }
        plugin.getLogger().info("Loaded " + islandNpcs.size() + " NPC data entries");
    }

    public void saveAllNpcData() {
        for (Map.Entry<UUID, Integer> entry : islandNpcs.entrySet()) {
            UUID islandUUID = entry.getKey();
            int npcId = entry.getValue();
            boolean hidden = hiddenNpcs.getOrDefault(islandUUID, false);

            String path = "npcs." + islandUUID.toString();
            plugin.getConfigManager().getNpcDataConfig().set(path + ".npc-id", npcId);
            plugin.getConfigManager().getNpcDataConfig().set(path + ".hidden", hidden);
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

        npc.spawn(spawnLoc);
        npc.data().setPersistent("islandUUID", islandUUID.toString());
        npc.data().setPersistent("dialogId", plugin.getConfigManager().getDialogId());

        setupHologram(npc);

        islandNpcs.put(islandUUID, npc.getId());
        hiddenNpcs.put(islandUUID, false);
        saveAllNpcData();

        plugin.getLogger().info("Created NPC for island: " + islandUUID);
        return npc;
    }

    private Location calculateSpawnLocation(Island island) {
        Location islandSpawn = island.getCenter(com.bgsoftware.superiorskyblock.api.world.Dimension.NORMAL);
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
