package com.magicbili.islandnpc.api;

import org.bukkit.Location;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;

/**
 * NPC提供者接口
 * 用于抽象不同的NPC插件（Citizens, FancyNpcs, ZNPCsPlus等）
 * 
 * @author magicbili
 */
public interface NpcProvider {
    
    /**
     * 获取提供者名称
     * @return 提供者名称（例如: "Citizens", "FancyNpcs"）
     */
    String getProviderName();
    
    /**
     * 为岛屿创建NPC
     * @param islandUUID 岛屿UUID
     * @param location NPC生成位置（已包含偏移量和朝向）
     * @return 是否创建成功
     */
    boolean createNpc(UUID islandUUID, Location location);
    
    /**
     * 删除岛屿的NPC
     * @param islandUUID 岛屿UUID
     * @return 是否删除成功
     */
    boolean deleteNpc(UUID islandUUID);
    
    /**
     * 隐藏岛屿的NPC
     * @param islandUUID 岛屿UUID
     * @return 是否隐藏成功
     */
    boolean hideNpc(UUID islandUUID);
    
    /**
     * 显示岛屿的NPC
     * @param islandUUID 岛屿UUID
     * @return 是否显示成功
     */
    boolean showNpc(UUID islandUUID);
    
    /**
     * 移动岛屿的NPC到新位置
     * @param islandUUID 岛屿UUID
     * @param newLocation 新位置
     * @return 是否移动成功
     */
    boolean moveNpc(UUID islandUUID, Location newLocation);
    
    /**
     * 检查NPC是否隐藏
     * @param islandUUID 岛屿UUID
     * @return 是否隐藏
     */
    boolean isNpcHidden(UUID islandUUID);
    
    /**
     * 检查岛屿是否有NPC
     * @param islandUUID 岛屿UUID
     * @return 是否有NPC
     */
    boolean hasNpc(UUID islandUUID);
    
    /**
     * 获取所有有NPC的岛屿UUID
     * @return 岛屿UUID集合
     */
    Set<UUID> getAllIslandUUIDs();
    
    /**
     * 从配置重新创建岛屿的NPC
     * 用于世界加载后恢复NPC
     * @param islandUUID 岛屿UUID
     * @return 是否重新创建成功
     */
    boolean recreateNpc(UUID islandUUID);
    
    /**
     * 重新加载所有NPC
     * 用于配置重载后更新NPC
     */
    void reloadAllNpcs();
    
    /**
     * 保存所有NPC数据
     */
    void saveAllNpcData();
    
    /**
     * 获取事件监听器
     * 某些NPC插件可能需要特定的事件监听器
     * @return 事件监听器，如果不需要返回null
     */
    Listener getEventListener();
    
    /**
     * 清理资源
     * 在插件禁用时调用
     */
    void cleanup();
}
