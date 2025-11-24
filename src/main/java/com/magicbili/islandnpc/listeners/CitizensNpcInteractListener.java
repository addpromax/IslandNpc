package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.magicbili.islandnpc.IslandNpcPlugin;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Citizens NPC 交互监听器
 * 只在 Citizens2 插件存在时加载
 */
public class CitizensNpcInteractListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    private final NpcInteractionHandler handler;
    
    public CitizensNpcInteractListener(IslandNpcPlugin plugin, NpcInteractionHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onNpcInteract(NPCRightClickEvent event) {
        if (event.isCancelled()) return;
        
        NPC npc = event.getNPC();
        Player player = event.getClicker();
        
        // 检查是否是岛屿 NPC
        String islandUUIDStr = npc.data().get("islandUUID");
        if (islandUUIDStr == null) return;
        
        try {
            UUID islandUUID = UUID.fromString(islandUUIDStr);
            
            // 验证玩家岛屿
            SuperiorPlayer sPlayer = SuperiorSkyblockAPI.getPlayer(player);
            Island playerIsland = sPlayer.getIsland();
            
            if (playerIsland == null || !playerIsland.getUniqueId().equals(islandUUID)) {
                player.sendMessage("§c这不是你的岛屿 NPC！");
                event.setCancelled(true);
                return;
            }
            
            // 处理交互
            boolean shouldCancel = handler.handleInteraction(player, islandUUID);
            if (shouldCancel) {
                event.setCancelled(true);
            }
            
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的岛屿 UUID: " + islandUUIDStr);
        }
    }
}
