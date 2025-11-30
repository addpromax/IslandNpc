package com.magicbili.islandnpc.hologram;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.HologramProvider;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FancyHolograms 全息图提供者
 * 
 * @author magicbili
 */
public class FancyHologramsProvider implements HologramProvider {
    
    private final IslandNpcPlugin plugin;
    private final Map<String, String> hologramIds; // 我们的ID -> FancyHolograms的ID
    
    public FancyHologramsProvider(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        this.hologramIds = new HashMap<>();
    }
    
    @Override
    public String getProviderName() {
        return "FancyHolograms";
    }
    
    @Override
    public boolean createHologram(String id, Location location, List<String> lines) {
        try {
            if (hologramExists(id)) {
                plugin.getLogger().warning("全息图已存在: " + id);
                return false;
            }
            
            // 创建全息图数据
            TextHologramData data = new TextHologramData(id, location);
            data.setText(lines);
            data.setPersistent(false); // 不持久化到配置文件
            data.setVisibilityDistance(50); // 可见距离50格
            
            // 创建全息图
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().create(data);
            if (hologram != null) {
                hologram.createHologram();
                
                // 添加到管理器 (全息图默认对所有人可见)
                FancyHologramsPlugin.get().getHologramManager().addHologram(hologram);
                
                hologramIds.put(id, hologram.getData().getName());
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("创建全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean deleteHologram(String id) {
        try {
            String fancyId = hologramIds.get(id);
            if (fancyId == null) {
                return false;
            }
            
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(fancyId).orElse(null);
            if (hologram != null) {
                hologram.deleteHologram();
                FancyHologramsPlugin.get().getHologramManager().removeHologram(hologram);
            }
            
            hologramIds.remove(id);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("删除全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean updateHologram(String id, List<String> lines) {
        try {
            String fancyId = hologramIds.get(id);
            if (fancyId == null) {
                return false;
            }
            
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(fancyId).orElse(null);
            if (hologram != null && hologram.getData() instanceof TextHologramData) {
                TextHologramData data = (TextHologramData) hologram.getData();
                data.setText(lines);
                // 重新创建全息图以应用更改
                hologram.deleteHologram();
                hologram.createHologram();
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("更新全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean moveHologram(String id, Location location) {
        try {
            String fancyId = hologramIds.get(id);
            if (fancyId == null) {
                return false;
            }
            
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(fancyId).orElse(null);
            if (hologram != null) {
                hologram.getData().setLocation(location);
                // 重新创建全息图以应用位置更改
                hologram.deleteHologram();
                hologram.createHologram();
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("移动全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean hologramExists(String id) {
        String fancyId = hologramIds.get(id);
        if (fancyId == null) {
            return false;
        }
        return FancyHologramsPlugin.get().getHologramManager().getHologram(fancyId).isPresent();
    }
    
    @Override
    public boolean hideHologram(String id) {
        try {
            String fancyId = hologramIds.get(id);
            if (fancyId == null) {
                return false;
            }
            
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(fancyId).orElse(null);
            if (hologram != null) {
                hologram.deleteHologram();
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("隐藏全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean showHologram(String id) {
        try {
            String fancyId = hologramIds.get(id);
            if (fancyId == null) {
                return false;
            }
            
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(fancyId).orElse(null);
            if (hologram != null) {
                hologram.createHologram();
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("显示全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void cleanup() {
        // 清理所有全息图
        for (String id : hologramIds.keySet()) {
            deleteHologram(id);
        }
        hologramIds.clear();
    }
    
    /**
     * 创建带背景的岛屿全息图 (FancyHolograms专用)
     */
    public boolean createIslandHologramWithBackground(String id, Location location, List<String> lines, ConfigurationSection config) {
        try {
            if (hologramExists(id)) {
                plugin.getLogger().warning("全息图已存在: " + id);
                return false;
            }
            
            // 创建全息图数据
            TextHologramData data = new TextHologramData(id, location);
            data.setText(lines);
            data.setPersistent(false);
            
            // 设置可见距离
            int viewRange = config.getInt("npc.hologram.view-range", 30);
            if (viewRange > 0) {
                data.setVisibilityDistance(viewRange);
            }
            
            // 设置背景
            boolean backgroundEnabled = config.getBoolean("npc.hologram.background.enabled", false);
            if (backgroundEnabled) {
                String colorHex = config.getString("npc.hologram.background.color", "0x40000000");
                try {
                    // 解析颜色值 (支持 0x 前缀) - ARGB格式
                    int argb = (int) Long.parseLong(colorHex.replace("0x", ""), 16);
                    // 转换为Bukkit Color对象 (支持Alpha通道)
                    int alpha = (argb >> 24) & 0xFF;
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    Color color = Color.fromARGB(alpha, red, green, blue);
                    data.setBackground(color);
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] 设置背景颜色: ARGB(" + alpha + "," + red + "," + green + "," + blue + ")");
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("无效的背景颜色值: " + colorHex);
                }
            } else {
                // 未启用背景时设置为完全透明
                data.setBackground(Color.fromARGB(0));
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] 背景设置为透明");
                }
            }
            
            // 创建全息图
            Hologram hologram = FancyHologramsPlugin.get().getHologramManager().create(data);
            if (hologram != null) {
                hologram.createHologram();
                
                // 添加到管理器 (全息图默认对所有人可见)
                FancyHologramsPlugin.get().getHologramManager().addHologram(hologram);
                
                hologramIds.put(id, hologram.getData().getName());
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] [FancyHolograms] 已创建全息图: " + id + 
                        ", 背景=" + backgroundEnabled + ", 可见距离=" + viewRange);
                }
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("创建全息图失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
