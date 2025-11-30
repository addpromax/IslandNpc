package com.magicbili.islandnpc.providers;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.api.world.Dimension;
import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.IslandProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * SuperiorSkyblock2 岛屿提供者实现
 * 
 * @author magicbili
 */
public class SuperiorSkyblockProvider implements IslandProvider {
    
    private final IslandNpcPlugin plugin;
    private SuperiorSkyblockListener listener;
    
    public SuperiorSkyblockProvider(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        // 延迟初始化 listener，因为此时 NpcProvider 可能还未创建
        // listener 会在 getEventListener() 被调用时初始化
    }
    
    @Override
    public String getProviderName() {
        return "SuperiorSkyblock2";
    }
    
    @Override
    public boolean hasIsland(Player player) {
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        return superiorPlayer.getIsland() != null;
    }
    
    @Override
    public UUID getIslandUUID(Player player) {
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        Island island = superiorPlayer.getIsland();
        return island != null ? island.getUniqueId() : null;
    }
    
    @Override
    public Location getIslandCenter(UUID islandUUID) {
        Island island = SuperiorSkyblockAPI.getIslandByUUID(islandUUID);
        if (island == null) {
            return null;
        }
        
        Dimension normalDimension = Dimension.getByName("NORMAL");
        return island.getCenter(normalDimension);
    }
    
    @Override
    public UUID getIslandOwner(UUID islandUUID) {
        Island island = SuperiorSkyblockAPI.getIslandByUUID(islandUUID);
        if (island == null || island.getOwner() == null) {
            return null;
        }
        return island.getOwner().getUniqueId();
    }
    
    @Override
    public String getIslandOwnerName(UUID islandUUID) {
        Island island = SuperiorSkyblockAPI.getIslandByUUID(islandUUID);
        if (island == null || island.getOwner() == null) {
            return null;
        }
        return island.getOwner().getName();
    }
    
    @Override
    public boolean islandExists(UUID islandUUID) {
        return SuperiorSkyblockAPI.getIslandByUUID(islandUUID) != null;
    }
    
    @Override
    public Listener getEventListener() {
        // 延迟初始化：确保 NpcProvider 已经创建
        if (listener == null) {
            listener = new SuperiorSkyblockListener(plugin, plugin.getNpcProvider());
            plugin.getLogger().info("已初始化 SuperiorSkyblock 事件监听器");
        }
        return listener;
    }
}
