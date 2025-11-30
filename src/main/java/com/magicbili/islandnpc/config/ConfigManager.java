package com.magicbili.islandnpc.config;

import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    // 当前配置文件版本号 - 更新配置时需要增加此版本号
    private static final int CURRENT_CONFIG_VERSION = 2;
    
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
        
        // 检查并更新配置文件版本
        checkAndUpdateConfig();

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
    
    /**
     * 检查配置文件版本并在需要时更新
     */
    private void checkAndUpdateConfig() {
        int configVersion = config.getInt("config-version", 0);
        
        if (configVersion == 0) {
            // 旧配置文件（没有版本号）
            plugin.getLogger().warning("检测到旧版本配置文件，正在自动更新...");
            migrateFromVersion0();
            config.set("config-version", CURRENT_CONFIG_VERSION);
            plugin.saveConfig();
            plugin.getLogger().info("配置文件已更新到版本 " + CURRENT_CONFIG_VERSION);
        } else if (configVersion < CURRENT_CONFIG_VERSION) {
            // 配置版本过旧，需要更新
            plugin.getLogger().warning("配置文件版本过旧 (当前: " + configVersion + ", 最新: " + CURRENT_CONFIG_VERSION + ")，正在自动更新...");
            
            // 根据版本号逐步迁移
            for (int version = configVersion; version < CURRENT_CONFIG_VERSION; version++) {
                migrateConfig(version, version + 1);
            }
            
            config.set("config-version", CURRENT_CONFIG_VERSION);
            plugin.saveConfig();
            plugin.getLogger().info("配置文件已更新到版本 " + CURRENT_CONFIG_VERSION);
        } else if (configVersion > CURRENT_CONFIG_VERSION) {
            // 配置版本比插件版本新（可能是降级）
            plugin.getLogger().warning("配置文件版本 (" + configVersion + ") 比插件版本 (" + CURRENT_CONFIG_VERSION + ") 更新！");
            plugin.getLogger().warning("这可能会导致问题。建议删除配置文件并重新生成。");
        } else {
            // 版本匹配，无需更新
            plugin.getLogger().info("配置文件版本: " + configVersion + " (最新)");
        }
    }
    
    /**
     * 从版本0（无版本号）迁移配置
     */
    private void migrateFromVersion0() {
        plugin.getLogger().info("正在从版本 0 迁移配置...");
        
        // 保存用户的所有自定义值
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
        
        try {
            // 备份旧配置
            if (configFile.exists()) {
                java.nio.file.Files.copy(
                    configFile.toPath(), 
                    backupFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                plugin.getLogger().info("已备份旧配置文件到 config.yml.backup");
            }
            
            // 读取用户的自定义值
            FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // 删除旧配置文件
            configFile.delete();
            
            // 生成新的默认配置
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            // 恢复用户的自定义值
            restoreUserSettings(oldConfig, config);
            
            plugin.saveConfig();
            plugin.getLogger().info("配置迁移完成");
            
        } catch (IOException e) {
            plugin.getLogger().severe("配置迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从一个版本迁移到另一个版本
     * @param fromVersion 源版本
     * @param toVersion 目标版本
     */
    private void migrateConfig(int fromVersion, int toVersion) {
        plugin.getLogger().info("正在从版本 " + fromVersion + " 迁移到版本 " + toVersion + "...");
        
        // 根据不同的版本进行特定的迁移操作
        switch (toVersion) {
            case 1:
                // 示例：如果将来需要从版本0到版本1的迁移
                // 添加新的配置项，保留旧的值
                if (!config.contains("npc.rotation")) {
                    config.set("npc.rotation.yaw", 180.0);
                    config.set("npc.rotation.pitch", 0.0);
                    plugin.getLogger().info("添加了新的配置项: npc.rotation");
                }
                if (!config.contains("npc.hologram.position")) {
                    config.set("npc.hologram.position.y-offset", 2.8);
                    config.set("npc.hologram.position.line-spacing", 0.3);
                    plugin.getLogger().info("添加了新的配置项: npc.hologram.position");
                }
                if (!config.contains("npc.hologram.background")) {
                    config.set("npc.hologram.background.enabled", false);
                    config.set("npc.hologram.background.color", "0x00000000");
                    plugin.getLogger().info("添加了新的配置项: npc.hologram.background");
                }
                break;
                
            case 2:
                // 从版本1到版本2的迁移: 添加全息图提供者配置
                if (!config.contains("npc.hologram.provider")) {
                    config.set("npc.hologram.provider", "FANCYHOLOGRAMS");
                    plugin.getLogger().info("添加了新的配置项: npc.hologram.provider");
                }
                break;
                
            // 未来版本的迁移逻辑在这里添加
            // case 3:
            //     // 从版本2到版本3的迁移
            //     break;
        }
        
        plugin.getLogger().info("迁移到版本 " + toVersion + " 完成");
    }
    
    /**
     * 恢复用户的自定义设置
     * @param oldConfig 旧配置
     * @param newConfig 新配置
     */
    private void restoreUserSettings(FileConfiguration oldConfig, FileConfiguration newConfig) {
        plugin.getLogger().info("正在恢复用户的自定义设置...");
        
        // NPC设置
        if (oldConfig.contains("npc.provider")) {
            newConfig.set("npc.provider", oldConfig.getString("npc.provider"));
        }
        if (oldConfig.contains("npc.entity-type")) {
            newConfig.set("npc.entity-type", oldConfig.getString("npc.entity-type"));
        }
        if (oldConfig.contains("npc.skin")) {
            newConfig.set("npc.skin", oldConfig.getString("npc.skin"));
        }
        if (oldConfig.contains("npc.dialog-id")) {
            newConfig.set("npc.dialog-id", oldConfig.getString("npc.dialog-id"));
        }
        
        // 生成位置偏移
        if (oldConfig.contains("npc.spawn-offset")) {
            newConfig.set("npc.spawn-offset", oldConfig.getConfigurationSection("npc.spawn-offset"));
        }
        
        // NPC朝向
        if (oldConfig.contains("npc.rotation")) {
            newConfig.set("npc.rotation", oldConfig.getConfigurationSection("npc.rotation"));
        }
        
        // 全息图设置
        if (oldConfig.contains("npc.hologram.enabled")) {
            newConfig.set("npc.hologram.enabled", oldConfig.getBoolean("npc.hologram.enabled"));
        }
        if (oldConfig.contains("npc.hologram.lines")) {
            newConfig.set("npc.hologram.lines", oldConfig.getStringList("npc.hologram.lines"));
        }
        if (oldConfig.contains("npc.hologram.position")) {
            newConfig.set("npc.hologram.position", oldConfig.getConfigurationSection("npc.hologram.position"));
        }
        if (oldConfig.contains("npc.hologram.background")) {
            newConfig.set("npc.hologram.background", oldConfig.getConfigurationSection("npc.hologram.background"));
        }
        if (oldConfig.contains("npc.hologram.view-range")) {
            newConfig.set("npc.hologram.view-range", oldConfig.getInt("npc.hologram.view-range"));
        }
        
        // 权限设置
        if (oldConfig.contains("permissions.default")) {
            newConfig.set("permissions.default", oldConfig.getBoolean("permissions.default"));
        }
        
        // 消息设置（保留所有自定义消息）
        if (oldConfig.contains("messages")) {
            for (String key : oldConfig.getConfigurationSection("messages").getKeys(false)) {
                newConfig.set("messages." + key, oldConfig.getString("messages." + key));
            }
        }
        
        // Debug模式
        if (oldConfig.contains("debug")) {
            newConfig.set("debug", oldConfig.getBoolean("debug"));
        }
        
        plugin.getLogger().info("用户自定义设置已恢复");
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

    /**
     * 获取NPC的Yaw旋转角度（水平方向）
     * @return Yaw角度（0-360）
     */
    public float getNpcYaw() {
        return (float) config.getDouble("npc.rotation.yaw", 180.0);
    }

    /**
     * 获取NPC的Pitch旋转角度（垂直方向）
     * @return Pitch角度（-90到90）
     */
    public float getNpcPitch() {
        return (float) config.getDouble("npc.rotation.pitch", 0.0);
    }

    public String getNpcEntityType() {
        return config.getString("npc.entity-type", "VILLAGER").toUpperCase();
    }

    public String getNpcSkin() {
        return config.getString("npc.skin", "");
    }

    public String getNpcProvider() {
        return config.getString("npc.provider", "CITIZENS").toUpperCase();
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

    /**
     * 获取全息图Y轴偏移量（相对于NPC头顶）
     * @return Y轴偏移量（格数）
     */
    public double getHologramYOffset() {
        return config.getDouble("npc.hologram.position.y-offset", 2.8);
    }

    /**
     * 获取全息图行间距
     * @return 行间距（格数）
     */
    public double getHologramLineSpacing() {
        return config.getDouble("npc.hologram.position.line-spacing", 0.3);
    }

    /**
     * 检查是否启用全息图背景
     * @return true 如果启用背景
     */
    public boolean isHologramBackgroundEnabled() {
        return config.getBoolean("npc.hologram.background.enabled", false);
    }

    /**
     * 获取全息图背景颜色（ARGB格式）
     * @return ARGB颜色值
     */
    public int getHologramBackgroundColor() {
        String colorStr = config.getString("npc.hologram.background.color", "0x00000000");
        try {
            // 支持 0x 开头的十六进制格式
            if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                return (int) Long.parseLong(colorStr.substring(2), 16);
            }
            return Integer.parseInt(colorStr);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("无效的全息图背景颜色配置: " + colorStr + "，使用默认透明色");
            return 0x00000000; // 默认完全透明
        }
    }

    /**
     * Get a message from config with color codes translated
     * @param key Message key from config
     * @return Colored message
     */
    public String getMessage(String key) {
        String message = config.getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Get a message with prefix
     * @param key Message key from config
     * @return Prefixed colored message
     */
    public String getMessageWithPrefix(String key) {
        return getPrefix() + getMessage(key);
    }

    /**
     * Get a message with placeholder replacements
     * @param key Message key from config
     * @param replacements Placeholder replacements (key, value pairs)
     * @return Colored message with replacements
     */
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Invalid placeholder replacements for message: " + key);
            return message;
        }
        
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            String value = replacements[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return message;
    }

    /**
     * Get the message prefix
     * @return Colored prefix
     */
    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', 
            config.getString("messages.prefix", "&8[&6IslandNpc&8]&r "));
    }

    /**
     * 检查是否启用Debug模式
     * @return true 如果启用Debug模式
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
}
