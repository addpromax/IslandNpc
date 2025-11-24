package com.magicbili.islandnpc;

import com.magicbili.islandnpc.commands.IslandNpcCommand;
import com.magicbili.islandnpc.config.ConfigManager;
import com.magicbili.islandnpc.listeners.PlayerIslandListener;
import com.magicbili.islandnpc.managers.NpcManager;
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
    private com.magicbili.islandnpc.managers.FancyNpcManager fancyNpcManager;

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置（必须在检查依赖之前，因为需要读取 NPC 提供者配置）
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 检查依赖插件并确定可用的NPC提供者
        String actualProvider = checkDependenciesAndDetermineProvider();
        if (actualProvider == null) {
            getLogger().severe("缺少必需的依赖插件！插件将被禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 根据实际可用的提供者初始化NPC管理器
        if ("FANCYNPCS".equals(actualProvider)) {
            fancyNpcManager = new com.magicbili.islandnpc.managers.FancyNpcManager(this);
            getLogger().info("使用 FancyNpcs 作为 NPC 提供者");
        } else {
            npcManager = new NpcManager(this);
            getLogger().info("使用 Citizens 作为 NPC 提供者");
        }

        // 注册监听器
        registerListeners();

        // 注册指令
        registerCommands();

        getLogger().info("IslandNpc 插件已启用！");
        getLogger().info("作者: magicbili");
    }

    @Override
    public void onDisable() {
        // 保存NPC数据
        if (npcManager != null) {
            npcManager.saveAllNpcData();
            getLogger().info("已保存 Citizens NPC 数据");
        }
        if (fancyNpcManager != null) {
            fancyNpcManager.saveAllNpcData();
            getLogger().info("已保存 FancyNpcs NPC 数据");
        }

        getLogger().info("IslandNpc 插件已禁用！");
    }

    /**
     * 检查必需的依赖插件并确定可用的NPC提供者
     * @return 可用的NPC提供者名称（CITIZENS 或 FANCYNPCS），如果都不可用则返回null
     */
    private String checkDependenciesAndDetermineProvider() {
        // 检查SuperiorSkyblock2
        if (Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") == null) {
            getLogger().severe("未找到 SuperiorSkyblock2 插件！");
            return null;
        } else {
            getLogger().info("已找到 SuperiorSkyblock2 插件");
        }

        // 检查可用的NPC提供者插件
        boolean hasCitizens = Bukkit.getPluginManager().getPlugin("Citizens") != null;
        boolean hasFancyNpcs = Bukkit.getPluginManager().getPlugin("FancyNpcs") != null;
        
        if (!hasCitizens && !hasFancyNpcs) {
            getLogger().severe("未找到任何 NPC 提供者插件！");
            getLogger().severe("请安装 Citizens 或 FancyNpcs 插件");
            return null;
        }
        
        // 获取配置中指定的提供者
        String configuredProvider = configManager.getNpcProvider();
        
        // 尝试使用配置的提供者
        if ("FANCYNPCS".equals(configuredProvider)) {
            if (hasFancyNpcs) {
                getLogger().info("已找到 FancyNpcs 插件");
                return "FANCYNPCS";
            } else {
                getLogger().warning("配置中指定使用 FancyNpcs，但未找到该插件");
                if (hasCitizens) {
                    getLogger().info("自动切换到 Citizens 插件");
                    return "CITIZENS";
                }
            }
        } else {
            // 配置为CITIZENS或其他值
            if (hasCitizens) {
                getLogger().info("已找到 Citizens 插件");
                return "CITIZENS";
            } else {
                getLogger().warning("配置中指定使用 Citizens，但未找到该插件");
                if (hasFancyNpcs) {
                    getLogger().info("自动切换到 FancyNpcs 插件");
                    return "FANCYNPCS";
                }
            }
        }
        
        // 检查FancyDialogs（可选）
        if (Bukkit.getPluginManager().getPlugin("FancyDialogs") != null) {
            getLogger().info("已找到 FancyDialogs 插件");
        } else {
            getLogger().warning("未找到 FancyDialogs 插件，对话功能将不可用");
        }
        
        return null;
    }

    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerIslandListener(this), this);
        
        // 注册 NPC 交互监听器 - 根据安装的插件动态注册
        // 此监听器处理所有 NPC 提供商的交互，并实现智能路由：
        // - 有任务时 → TypeWriter 处理
        // - 无任务时 → 打开 FancyDialogs 菜单
        com.magicbili.islandnpc.listeners.NpcInteractionHandler handler = 
            new com.magicbili.islandnpc.listeners.NpcInteractionHandler(this);
        
        // 只在对应插件存在时注册监听器
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            try {
                getServer().getPluginManager().registerEvents(
                    new com.magicbili.islandnpc.listeners.CitizensNpcInteractListener(this, handler), 
                    this
                );
                getLogger().info("已注册 Citizens NPC 交互监听器");
            } catch (Exception e) {
                getLogger().warning("注册 Citizens 监听器失败: " + e.getMessage());
            }
        }
        
        if (Bukkit.getPluginManager().getPlugin("FancyNpcs") != null) {
            try {
                getServer().getPluginManager().registerEvents(
                    new com.magicbili.islandnpc.listeners.FancyNpcsInteractListener(this, handler), 
                    this
                );
                getLogger().info("已注册 FancyNpcs NPC 交互监听器");
            } catch (Exception e) {
                getLogger().warning("注册 FancyNpcs 监听器失败: " + e.getMessage());
            }
        }
        
        // 世界加载监听器 - 只在 ASP 插件存在时注册
        if (Bukkit.getPluginManager().getPlugin("AdvancedSlimePaper") != null) {
            try {
                getServer().getPluginManager().registerEvents(
                    new com.magicbili.islandnpc.listeners.WorldLoadListener(this), 
                    this
                );
                getLogger().info("已注册世界加载监听器");
            } catch (Exception e) {
                getLogger().warning("注册世界加载监听器失败: " + e.getMessage());
            }
        }
        
        getLogger().info("事件监听器注册完成");
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
        if (npcManager != null) {
            npcManager.reloadAllNpcs();
        }
        if (fancyNpcManager != null) {
            fancyNpcManager.reloadAllNpcs();
        }
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

    public com.magicbili.islandnpc.managers.FancyNpcManager getFancyNpcManager() {
        return fancyNpcManager;
    }

}
