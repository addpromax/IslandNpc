package com.magicbili.islandnpc.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * 岛屿提供者接口
 * 用于抽象不同的岛屿插件（SuperiorSkyblock2, BentoBox等）
 * 
 * @author magicbili
 */
public interface IslandProvider {
    
    /**
     * 获取提供者名称
     * @return 提供者名称（例如: "SuperiorSkyblock2", "BentoBox"）
     */
    String getProviderName();
    
    /**
     * 检查玩家是否有岛屿
     * @param player 玩家
     * @return 如果玩家有岛屿返回true
     */
    boolean hasIsland(Player player);
    
    /**
     * 获取玩家的岛屿UUID
     * @param player 玩家
     * @return 岛屿UUID，如果没有岛屿返回null
     */
    UUID getIslandUUID(Player player);
    
    /**
     * 获取岛屿的中心位置
     * @param islandUUID 岛屿UUID
     * @return 岛屿中心位置，如果岛屿不存在返回null
     */
    Location getIslandCenter(UUID islandUUID);
    
    /**
     * 获取岛屿拥有者的UUID
     * @param islandUUID 岛屿UUID
     * @return 岛屿拥有者UUID，如果岛屿不存在返回null
     */
    UUID getIslandOwner(UUID islandUUID);
    
    /**
     * 获取岛屿拥有者的名称
     * @param islandUUID 岛屿UUID
     * @return 岛屿拥有者名称，如果岛屿不存在返回null
     */
    String getIslandOwnerName(UUID islandUUID);
    
    /**
     * 检查岛屿是否存在
     * @param islandUUID 岛屿UUID
     * @return 如果岛屿存在返回true
     */
    boolean islandExists(UUID islandUUID);
    
    /**
     * 获取事件监听器
     * 每个提供者需要实现自己的事件监听器来处理岛屿创建、删除等事件
     * @return 事件监听器
     */
    Listener getEventListener();
}
