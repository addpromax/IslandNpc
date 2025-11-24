package com.magicbili.islandnpc.listeners;

import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * NPC 交互处理器
 * 处理所有 NPC 提供商的统一交互逻辑
 * 
 * 交互逻辑：
 * 1. 检测玩家是否有与该 NPC 相关的 TypeWriter 任务
 * 2. 如果有任务 → 返回 false（不取消事件，让 TypeWriter 处理）
 * 3. 如果没有任务 → 打开 FancyDialogs 菜单，返回 true（取消事件）
 */
public class NpcInteractionHandler {
    
    private final IslandNpcPlugin plugin;
    
    public NpcInteractionHandler(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 处理 NPC 交互
     * @return true 如果应该取消事件，false 如果应该让其他插件处理
     */
    public boolean handleInteraction(Player player, UUID islandUUID) {
        debug("玩家 " + player.getName() + " 与岛屿 NPC 交互");
        
        // 检查是否有 TypeWriter 任务
        boolean hasQuest = checkTypeWriterQuest(player, islandUUID);
        
        if (hasQuest) {
            // 有任务 - 让 TypeWriter 处理交互
            debug("触发 TypeWriter 交互");
            return false; // 不取消事件
        } else {
            // 没有任务 - 打开 FancyDialogs 菜单
            debug("打开 FancyDialogs 菜单");
            openFancyDialogsMenu(player, islandUUID);
            return true; // 取消事件，防止其他交互
        }
    }
    
    /**
     * 检查玩家是否有 TypeWriter 任务并触发
     */
    private boolean checkTypeWriterQuest(Player player, UUID islandUUID) {
        try {
            // 方法 1: 使用已注册的 TypeWriter 桥接服务（推荐）
            com.magicbili.islandnpc.api.TypeWriterBridge bridge = 
                com.magicbili.islandnpc.api.TypeWriterServiceRegistry.getBridge();
            
            if (bridge != null) {
                if (bridge.hasActiveEvent(player, islandUUID)) {
                    debug("检测到活跃的 TypeWriter 岛屿 NPC 事件");
                    bridge.triggerEvents(player, islandUUID);
                    debug("已触发 TypeWriter 事件");
                    return true;
                }
            } else {
                debug("TypeWriter Extension 服务未注册");
            }
            
            // 方法 2: 检查玩家是否有特定的元数据标记
            if (player.hasMetadata("typewriter_quest_active")) {
                debug("玩家有活跃的 TypeWriter 任务标记");
                return true;
            }
            
            // 方法 3: 检查玩家的权限或临时数据
            if (player.hasPermission("typewriter.quest.active")) {
                debug("玩家有任务权限标记");
                return true;
            }
            
            debug("玩家没有活跃的任务");
            return false;
            
        } catch (Exception e) {
            debug("检查任务失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 打开 FancyDialogs 菜单
     */
    private void openFancyDialogsMenu(Player player, UUID islandUUID) {
        String dialogId = plugin.getConfigManager().getDialogId();
        
        if (dialogId == null || dialogId.isEmpty()) {
            debug("对话 ID 未配置");
            player.sendMessage("§c对话 ID 未配置！");
            return;
        }
        
        // 检查 FancyDialogs 是否存在
        if (Bukkit.getPluginManager().getPlugin("FancyDialogs") == null) {
            debug("FancyDialogs 插件未安装");
            player.sendMessage("§c菜单系统未安装！");
            return;
        }
        
        // 执行打开对话的命令
        debug("打开对话: " + dialogId);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
            "fancydialogs open " + dialogId + " " + player.getName());
    }
    
    /**
     * 输出debug日志
     */
    private void debug(String message) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] [交互] " + message);
        }
    }
}
