package com.magicbili.islandnpc.hologram;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.HologramProvider;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DecentHolograms 全息图提供者
 * 
 * @author magicbili
 */
public class DecentHologramsProvider implements HologramProvider {
    
    private final IslandNpcPlugin plugin;
    private final Map<String, Hologram> holograms;
    
    public DecentHologramsProvider(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }
    
    @Override
    public String getProviderName() {
        return "DecentHolograms";
    }
    
    @Override
    public boolean createHologram(String id, Location location, List<String> lines) {
        try {
            if (hologramExists(id)) {
                plugin.getLogger().warning("全息图已存在: " + id);
                return false;
            }
            
            // 创建全息图
            Hologram hologram = DHAPI.createHologram(id, location, lines);
            if (hologram != null) {
                holograms.put(id, hologram);
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
            Hologram hologram = holograms.get(id);
            if (hologram != null) {
                hologram.delete();
                holograms.remove(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("删除全息图失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean updateHologram(String id, List<String> lines) {
        try {
            Hologram hologram = holograms.get(id);
            if (hologram != null) {
                DHAPI.setHologramLines(hologram, lines);
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
            Hologram hologram = holograms.get(id);
            if (hologram != null) {
                DHAPI.moveHologram(hologram, location);
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
        return holograms.containsKey(id) && DHAPI.getHologram(id) != null;
    }
    
    @Override
    public boolean hideHologram(String id) {
        try {
            Hologram hologram = holograms.get(id);
            if (hologram != null) {
                // DecentHolograms 需要对每个在线玩家隐藏
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    hologram.hide(player);
                }
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
            Hologram hologram = holograms.get(id);
            if (hologram != null) {
                // DecentHolograms 需要对每个在线玩家显示
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    hologram.show(player, 0);
                }
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
        for (String id : holograms.keySet()) {
            deleteHologram(id);
        }
        holograms.clear();
    }
}
