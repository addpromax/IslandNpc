package com.magicbili.islandnpc.listeners;

import com.bgsoftware.superiorskyblock.api.events.IslandCreateEvent;
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
            if (island != null && island.getCenter(com.bgsoftware.superiorskyblock.api.world.Dimension.NORMAL) != null) {
                plugin.getNpcManager().createIslandNpc(island);
            }
        }, 20L);
    }
}
