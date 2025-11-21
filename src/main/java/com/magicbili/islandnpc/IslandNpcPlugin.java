package com.magicbili.islandnpc;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.magicbili.islandnpc.commands.IslandNpcCommand;
import com.magicbili.islandnpc.config.ConfigManager;
import com.magicbili.islandnpc.listeners.NpcInteractListener;
import com.magicbili.islandnpc.listeners.PlayerIslandListener;
import com.magicbili.islandnpc.managers.NpcManager;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * IslandNpc Plugin - 岛屿NPC插件
 * 为SuperiorSkyblock2岛屿创建和管理NPC
 * 
 * @author magicbili
 */
public class IslandNpcPlugin extends JavaPlugin {

    private static IslandNpcPlugin instance;
    private ConfigManager configManager;
    private NpcManager npcManager;

    @Override
    public void onEnable() {
        instance = this;

        // 检查依赖插件
        if (!checkDependencies()) {
            getLogger().severe("缺少必需的依赖插件！插件将被禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化配置
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 初始化NPC管理器
        npcManager = new NpcManager(this);

        // 注册监听器
        registerListeners();

        // 注册指令
        registerCommands();

        getLogger().info("IslandNpc 插件已启用！");
        getLogger().info("作者: magicbili");
    }

    @Override
    public void onDisable() {
        // 保存所有NPC数据
        if (npcManager != null) {
            npcManager.saveAllNpcData();
        }

        getLogger().info("IslandNpc 插件已禁用！");
    }

    /**
     * 检查必需的依赖插件
     */
    private boolean checkDependencies() {
        boolean hasAll = true;

        // 检查SuperiorSkyblock2
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") == null) {
            getLogger().severe("未找到 SuperiorSkyblock2 插件！");
            hasAll = false;
        } else {
            getLogger().info("已找到 SuperiorSkyblock2 插件");
        }

        // 检查Citizens
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            getLogger().severe("未找到 Citizens 插件！");
            hasAll = false;
        } else {
            getLogger().info("已找到 Citizens 插件");
        }

        // 检查FancyDialogs（可选）
        if (Bukkit.getPluginManager().getPlugin("FancyDialogs") != null) {
            getLogger().info("已找到 FancyDialogs 插件");
        } else {
            getLogger().warning("未找到 FancyDialogs 插件，对话功能将不可用");
        }

        return hasAll;
    }

    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerIslandListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
        getLogger().info("事件监听器已注册");
    }

    /**
     * 注册指令
     */
    private void registerCommands() {
        IslandNpcCommand commandHandler = new IslandNpcCommand(this);
        getCommand("islandnpc").setExecutor(commandHandler);
        getCommand("islandnpc").setTabCompleter(commandHandler);
        getLogger().info("指令已注册");
    }

    /**
     * 重载插件配置
     */
    public void reloadPlugin() {
        configManager.loadConfig();
        npcManager.reloadAllNpcs();
        getLogger().info("插件配置已重载");
    }

    public static IslandNpcPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }
}
