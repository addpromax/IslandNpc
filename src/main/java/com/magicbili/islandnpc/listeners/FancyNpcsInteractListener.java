package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.magicbili.islandnpc.IslandNpcPlugin;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * FancyNpcs NPC 交互监听器
 * 只在 FancyNpcs 插件存在时加载
 */
public class FancyNpcsInteractListener implements Listener {
    
    private final IslandNpcPlugin plugin;
    private final NpcInteractionHandler handler;
    
    public FancyNpcsInteractListener(IslandNpcPlugin plugin, NpcInteractionHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onNpcInteract(NpcInteractEvent event) {
        if (event.isCancelled()) return;
        
        de.oliver.fancynpcs.api.Npc npc = event.getNpc();
        Player player = event.getPlayer();
        
        // 获取岛屿 UUID（从 NPC 数据获取）
        String npcId = npc.getData().getId();
        UUID islandUUID = findIslandUUIDByNpcId(npcId);
        
        // 如果不是岛屿 NPC，直接返回，不处理
        if (islandUUID == null) {
            return;
        }
        
        // 验证玩家岛屿
        SuperiorPlayer sPlayer = SuperiorSkyblockAPI.getPlayer(player);
        Island playerIsland = sPlayer.getIsland();
        
        if (playerIsland == null || !playerIsland.getUniqueId().equals(islandUUID)) {
            player.sendMessage("§c这不是你的岛屿 NPC！");
            event.setCancelled(true);
            return;
        }
        
        // 处理交互
        handler.handleInteraction(player, islandUUID);
    }
    
    /**
     * 通过NPC ID查找岛屿UUID
     */
    private UUID findIslandUUIDByNpcId(String npcId) {
        if (plugin.getNpcProvider() == null) return null;
        
        // 检查是否是 FancyNpcProvider
        if (plugin.getNpcProvider() instanceof com.magicbili.islandnpc.npc.FancyNpcProvider) {
            com.magicbili.islandnpc.npc.FancyNpcProvider fancyProvider = 
                (com.magicbili.islandnpc.npc.FancyNpcProvider) plugin.getNpcProvider();
            return fancyProvider.getIslandUUIDByNpcId(npcId);
        }
        
        return null;
    }
}
