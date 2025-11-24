package com.magicbili.islandnpc.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * TypeWriter Extension 桥接接口
 * Extension 通过此接口向主插件注册服务
 */
public interface TypeWriterBridge {
    
    /**
     * 检查玩家是否有活跃的 TypeWriter 事件
     * 
     * @param player 玩家
     * @param islandUUID 岛屿 UUID
     * @return 是否有活跃事件
     */
    boolean hasActiveEvent(Player player, UUID islandUUID);
    
    /**
     * 触发 TypeWriter 事件
     * 
     * @param player 玩家
     * @param islandUUID 岛屿 UUID
     */
    void triggerEvents(Player player, UUID islandUUID);
}
