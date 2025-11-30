package com.magicbili.islandnpc.utils;

import org.bukkit.World;

/**
 * 世界工具类
 * 提供世界类型检测等功能
 * 
 * @author magicbili
 */
public class WorldUtils {
    
    private static boolean aspApiAvailable = false;
    
    static {
        try {
            Class.forName("com.infernalsuite.asp.api.AdvancedSlimePaperAPI");
            aspApiAvailable = true;
        } catch (ClassNotFoundException e) {
            // ASP API 不可用
        }
    }
    
    /**
     * 检测世界是否为 SlimeWorld
     * 
     * @param world 要检测的世界
     * @return true 如果是 SlimeWorld，false 如果是普通世界或无法判断
     */
    public static boolean isSlimeWorld(World world) {
        if (world == null) {
            return false;
        }
        
        if (!aspApiAvailable) {
            return false;
        }
        
        try {
            // 使用 ASP API 检测
            com.infernalsuite.asp.api.AdvancedSlimePaperAPI api = 
                com.infernalsuite.asp.api.AdvancedSlimePaperAPI.instance();
            
            if (api == null) {
                return false;
            }
            
            // 检查世界是否在已加载的 SlimeWorld 列表中
            return api.getLoadedWorld(world.getName()) != null;
        } catch (Exception e) {
            // 如果出现任何异常，假定为普通世界
            return false;
        }
    }
    
    /**
     * 检测 ASP API 是否可用
     * 
     * @return true 如果 ASP API 可用
     */
    public static boolean isAspApiAvailable() {
        return aspApiAvailable;
    }
}
