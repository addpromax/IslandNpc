package com.magicbili.islandnpc.api;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

/**
 * 全息图提供者接口
 * 用于抽象不同的全息图插件(FancyHolograms, DecentHolograms等)
 * 
 * @author magicbili
 */
public interface HologramProvider {
    
    /**
     * 获取提供者名称
     * @return 提供者名称
     */
    String getProviderName();
    
    /**
     * 创建全息图
     * @param id 全息图唯一标识
     * @param location 位置
     * @param lines 文本行
     * @return 是否创建成功
     */
    boolean createHologram(String id, Location location, List<String> lines);
    
    /**
     * 删除全息图
     * @param id 全息图唯一标识
     * @return 是否删除成功
     */
    boolean deleteHologram(String id);
    
    /**
     * 更新全息图内容
     * @param id 全息图唯一标识
     * @param lines 新的文本行
     * @return 是否更新成功
     */
    boolean updateHologram(String id, List<String> lines);
    
    /**
     * 移动全息图位置
     * @param id 全息图唯一标识
     * @param location 新位置
     * @return 是否移动成功
     */
    boolean moveHologram(String id, Location location);
    
    /**
     * 检查全息图是否存在
     * @param id 全息图唯一标识
     * @return 是否存在
     */
    boolean hologramExists(String id);
    
    /**
     * 隐藏全息图
     * @param id 全息图唯一标识
     * @return 是否隐藏成功
     */
    boolean hideHologram(String id);
    
    /**
     * 显示全息图
     * @param id 全息图唯一标识
     * @return 是否显示成功
     */
    boolean showHologram(String id);
    
    /**
     * 清理所有全息图
     */
    void cleanup();
    
    /**
     * 为岛屿NPC创建全息图
     * @param islandUUID 岛屿UUID
     * @param location NPC位置
     * @param ownerName 岛主名称
     * @return 是否创建成功
     */
    default boolean createIslandHologram(UUID islandUUID, Location location, String ownerName) {
        String id = "island_" + islandUUID.toString();
        Location hologramLoc = location.clone().add(0, 2.5, 0); // NPC上方2.5格
        
        List<String> lines = List.of(
            "§b§l" + ownerName + " 的岛屿",
            "§7右键点击交互"
        );
        
        return createHologram(id, hologramLoc, lines);
    }
    
    /**
     * 删除岛屿全息图
     * @param islandUUID 岛屿UUID
     * @return 是否删除成功
     */
    default boolean deleteIslandHologram(UUID islandUUID) {
        String id = "island_" + islandUUID.toString();
        return deleteHologram(id);
    }
}
