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
        
        // 延迟加载所有已存在的 NPC（模仿 FancyNpcs 的做法）
        // 延迟 5 秒确保所有插件和世界都已完全加载
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (configManager.isDebugEnabled()) {
                getLogger().info("[DEBUG] 开始加载所有已存在世界中的 NPC...");
            }
            loadAllExistingNpcs();
        }, 100L); // 5 秒延迟

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
            // 清理所有全息图，防止内存泄漏
            fancyNpcManager.cleanupAllHolograms();
            getLogger().info("已保存 FancyNpcs NPC 数据并清理全息图");
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
        // - 无任务时 → 不处理，让其他插件处理
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
        
        // 世界加载监听器 - 直接检测 API 类是否存在，而不是检测插件名称
        // SlimeWorld 是核心功能，不是插件，所以应该检测 API 类
        try {
            // 尝试加载 LoadSlimeWorldEvent 类，如果存在说明 ASP API 可用
            Class.forName("com.infernalsuite.asp.api.events.LoadSlimeWorldEvent");
            
            // API 可用，注册监听器
            com.magicbili.islandnpc.listeners.WorldLoadListener worldListener = 
                new com.magicbili.islandnpc.listeners.WorldLoadListener(this);
            getServer().getPluginManager().registerEvents(worldListener, this);
            getLogger().info("已注册世界加载监听器 (支持 SlimeWorld 和普通岛屿世界)");
            
            // 输出当前已加载的世界信息（用于调试）
            if (configManager.isDebugEnabled()) {
                getLogger().info("[DEBUG] 当前已加载的世界:");
                for (org.bukkit.World w : Bukkit.getWorlds()) {
                    getLogger().info("[DEBUG]   - " + w.getName() + " (环境: " + w.getEnvironment() + ")");
                }
            }
        } catch (ClassNotFoundException e) {
            // API 不可用，跳过注册（静默处理，不输出日志）
        } catch (Exception e) {
            getLogger().severe("注册世界加载监听器失败: " + e.getMessage());
            e.printStackTrace();
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
    
    /**
     * 加载所有当前已加载世界中的 NPC
     * 在插件启动时调用，延迟 5 秒确保所有插件和世界都已加载
     * 
     * 优化策略：
     * 1. 只检查当前已加载的世界（避免遍历所有数据源）
     * 2. 未加载的世界由世界加载事件处理（包括 SlimeWorld 和普通世界）
     * 3. 性能友好：即使有成千上万个岛屿配置，也只处理已加载的世界
     */
    private void loadAllExistingNpcs() {
        org.bukkit.configuration.ConfigurationSection section = configManager.getNpcDataConfig().getConfigurationSection("npcs");
        if (section == null) {
            if (configManager.isDebugEnabled()) {
                getLogger().info("[DEBUG] 配置中没有 npcs 节点，无需加载");
            }
            return;
        }
        
        // 获取当前所有已加载的世界名称（性能优化：使用 Set 快速查找）
        java.util.Set<String> loadedWorldNames = new java.util.HashSet<>();
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            loadedWorldNames.add(world.getName());
        }
        
        if (configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] 当前已加载 " + loadedWorldNames.size() + " 个世界");
        }
        
        int loadedCount = 0;
        int skippedCount = 0;
        
        for (String key : section.getKeys(false)) {
            try {
                java.util.UUID islandUUID = java.util.UUID.fromString(key);
                String worldName = section.getString(key + ".location.world");
                
                if (worldName == null) {
                    getLogger().warning("岛屿 " + key + " 的世界名称为空，跳过");
                    continue;
                }
                
                // 只处理已加载的世界，未加载的世界由世界加载事件处理
                if (!loadedWorldNames.contains(worldName)) {
                    if (configManager.isDebugEnabled()) {
                        getLogger().info("[DEBUG] 世界未加载，跳过: " + worldName + " (将由世界加载事件处理)");
                    }
                    skippedCount++;
                    continue;
                }
                
                // 使用对应的 NPC 管理器重新创建 NPC
                if (fancyNpcManager != null) {
                    fancyNpcManager.recreateNpcForIsland(islandUUID);
                    loadedCount++;
                } else if (npcManager != null) {
                    npcManager.recreateNpcForIsland(islandUUID);
                    loadedCount++;
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("无效的岛屿UUID: " + key);
            } catch (Exception e) {
                getLogger().severe("加载岛屿 " + key + " 的 NPC 时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (loadedCount > 0) {
            getLogger().info("成功加载了 " + loadedCount + " 个已加载世界的 NPC");
        }
        if (skippedCount > 0 && configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] 跳过了 " + skippedCount + " 个未加载的世界（将由世界加载事件处理）");
        }
        if (loadedCount == 0 && skippedCount == 0 && configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] 没有需要加载的 NPC");
        }
    }

}
