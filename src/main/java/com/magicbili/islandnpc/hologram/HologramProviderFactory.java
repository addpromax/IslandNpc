package com.magicbili.islandnpc.hologram;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.HologramProvider;
import org.bukkit.Bukkit;

/**
 * 全息图提供者工厂
 * 
 * @author magicbili
 */
public class HologramProviderFactory {
    
    /**
     * 创建全息图提供者
     * @param plugin 插件实例
     * @return 全息图提供者,如果没有可用的或被禁用则返回null
     */
    public static HologramProvider createProvider(IslandNpcPlugin plugin) {
        // 检查配置是否启用全息图
        if (!plugin.getConfigManager().getConfig().getBoolean("npc.hologram.enabled", true)) {
            plugin.getLogger().info("全息图功能已在配置中禁用");
            return null;
        }
        
        String configuredProvider = plugin.getConfigManager().getConfig().getString("npc.hologram.provider", "FANCYHOLOGRAMS");
        boolean hasFancyHolograms = Bukkit.getPluginManager().getPlugin("FancyHolograms") != null;
        boolean hasDecentHolograms = Bukkit.getPluginManager().getPlugin("DecentHolograms") != null;
        
        // 检查是否有可用的全息图插件
        if (!hasFancyHolograms && !hasDecentHolograms) {
            plugin.getLogger().warning("未找到全息图插件,NPC将不会显示全息图");
            plugin.getLogger().warning("支持的全息图插件: FancyHolograms, DecentHolograms");
            return null;
        }
        
        // 如果两个插件都找到了，使用配置文件中的设置
        if (hasFancyHolograms && hasDecentHolograms) {
            if ("DECENTHOLOGRAMS".equalsIgnoreCase(configuredProvider)) {
                plugin.getLogger().info("检测到 FancyHolograms 和 DecentHolograms，使用配置: DecentHolograms");
                return new DecentHologramsProvider(plugin);
            } else {
                plugin.getLogger().info("检测到 FancyHolograms 和 DecentHolograms，使用配置: FancyHolograms");
                return new FancyHologramsProvider(plugin);
            }
        }
        
        // 只找到一个插件，自动使用
        if (hasFancyHolograms) {
            plugin.getLogger().info("检测到 FancyHolograms，自动使用");
            return new FancyHologramsProvider(plugin);
        } else {
            plugin.getLogger().info("检测到 DecentHolograms，自动使用");
            return new DecentHologramsProvider(plugin);
        }
    }
}
