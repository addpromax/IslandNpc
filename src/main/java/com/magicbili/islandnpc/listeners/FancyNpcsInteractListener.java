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
        if (islandUUID == null) return;
        
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
        
        for (UUID islandUUID : plugin.getNpcProvider().getAllIslandUUIDs()) {
            // 这里需要检查NPC是否属于这个岛屿
            // 由于FancyNpcProvider使用岛屿UUID作为映射,我们可以直接检查
            if (plugin.getNpcProvider().hasNpc(islandUUID)) {
                return islandUUID;
            }
        }
        return null;
    }
}
