package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.events.IslandCreateEvent;
import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.magicbili.islandnpc.IslandNpcPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PlayerIslandListener implements Listener {

    private final IslandNpcPlugin plugin;

    public PlayerIslandListener(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandCreate(IslandCreateEvent event) {
        Island island = event.getIsland();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (island != null) {
                com.bgsoftware.superiorskyblock.api.world.Dimension normalDimension = 
                    com.bgsoftware.superiorskyblock.api.world.Dimension.getByName("NORMAL");
                if (island.getCenter(normalDimension) != null) {
                    plugin.getNpcManager().createIslandNpc(island);
                }
            }
        }, 20L);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        Island island = event.getIsland();
        if (island != null) {
            // 异步删除NPC，避免阻塞主线程
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getNpcManager().deleteNpc(island.getUniqueId());
            });
        }
    }
}
