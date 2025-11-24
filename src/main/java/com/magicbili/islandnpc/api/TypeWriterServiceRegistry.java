package com.magicbili.islandnpc.api;

import org.bukkit.Bukkit;

/**
 * TypeWriter 服务注册器
 * Extension 通过此类注册服务
 */
public class TypeWriterServiceRegistry {
    
    private static TypeWriterBridge registeredBridge = null;
    
    /**
     * 注册 TypeWriter 桥接服务
     * 由 TypeWriter Extension 调用
     * 
     * @param bridge 桥接实现
     */
    public static void register(TypeWriterBridge bridge) {
        registeredBridge = bridge;
        Bukkit.getLogger().info("[IslandNpc] TypeWriter 集成已启用");
    }
    
    /**
     * 取消注册服务
     */
    public static void unregister() {
        registeredBridge = null;
    }
    
    /**
     * 获取已注册的服务
     * 
     * @return 桥接服务，如果未注册则返回 null
     */
    public static TypeWriterBridge getBridge() {
        return registeredBridge;
    }
    
    /**
     * 检查服务是否已注册
     * 
     * @return 是否已注册
     */
    public static boolean isRegistered() {
        return registeredBridge != null;
    }
}
