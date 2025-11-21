package com.magicbili.islandnpc.config;

import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final IslandNpcPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration npcDataConfig;
    private File npcDataFile;

    public ConfigManager(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        npcDataFile = new File(plugin.getDataFolder(), "npcdata.yml");
        if (!npcDataFile.exists()) {
            try {
                npcDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Cannot create npcdata.yml: " + e.getMessage());
            }
        }
        npcDataConfig = YamlConfiguration.loadConfiguration(npcDataFile);
    }

    public void saveNpcData() {
        try {
            npcDataConfig.save(npcDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Cannot save npcdata.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getNpcDataConfig() {
        return npcDataConfig;
    }

    public String getDialogId() {
        return config.getString("npc.dialog-id", "default_dialog");
    }

    public double getSpawnOffsetX() {
        return config.getDouble("npc.spawn-offset.x", 0.0);
    }

    public double getSpawnOffsetY() {
        return config.getDouble("npc.spawn-offset.y", 0.0);
    }

    public double getSpawnOffsetZ() {
        return config.getDouble("npc.spawn-offset.z", 5.0);
    }

    public String getNpcEntityType() {
        return config.getString("npc.entity-type", "VILLAGER").toUpperCase();
    }

    public String getNpcSkin() {
        return config.getString("npc.skin", "");
    }

    public boolean isDefaultPermissions() {
        return config.getBoolean("permissions.default", true);
    }

    public boolean isHologramEnabled() {
        return config.getBoolean("npc.hologram.enabled", true);
    }

    public java.util.List<String> getHologramLines() {
        return config.getStringList("npc.hologram.lines");
    }

    public double getHologramLineHeight() {
        return config.getDouble("npc.hologram.line-height", -1);
    }

    public int getHologramViewRange() {
        return config.getInt("npc.hologram.view-range", 30);
    }
}
