package com.magicbili.islandnpc.npc;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.NpcProvider;
import org.bukkit.Bukkit;

/**
 * NPC提供者工厂类
 * 根据配置和可用插件创建合适的NPC提供者
 * 
 * @author magicbili
 */
public class NpcProviderFactory {
    
    /**
     * 创建NPC提供者
     * @param plugin 插件实例
     * @return NPC提供者实例，如果没有可用的提供者返回null
     */
    public static NpcProvider createProvider(IslandNpcPlugin plugin) {
        String configuredProvider = plugin.getConfigManager().getNpcProvider();
        
        boolean hasCitizens = Bukkit.getPluginManager().getPlugin("Citizens") != null;
        boolean hasFancyNpcs = Bukkit.getPluginManager().getPlugin("FancyNpcs") != null;
        
        // 检查是否有可用的NPC插件
        if (!hasCitizens && !hasFancyNpcs) {
            plugin.getLogger().severe("未找到 NPC 提供者插件！请安装 Citizens 或 FancyNpcs");
            return null;
        }
        
        // 如果两个插件都找到了，使用配置文件中的设置
        if (hasCitizens && hasFancyNpcs) {
            if ("FANCYNPCS".equalsIgnoreCase(configuredProvider)) {
                plugin.getLogger().info("检测到 Citizens 和 FancyNpcs，使用配置: FancyNpcs");
                return new FancyNpcProvider(plugin);
            } else {
                plugin.getLogger().info("检测到 Citizens 和 FancyNpcs，使用配置: Citizens");
                return new CitizensNpcProvider(plugin);
            }
        }
        
        // 只找到一个插件，自动使用
        if (hasFancyNpcs) {
            plugin.getLogger().info("检测到 FancyNpcs，自动使用");
            return new FancyNpcProvider(plugin);
        } else {
            plugin.getLogger().info("检测到 Citizens，自动使用");
            return new CitizensNpcProvider(plugin);
        }
    }
    
    /**
     * 获取NPC提供者名称（用于日志）
     * @param provider NPC提供者
     * @return 提供者名称
     */
    public static String getProviderName(NpcProvider provider) {
        return provider != null ? provider.getProviderName() : "None";
    }
}
