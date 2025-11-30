package com.magicbili.islandnpc.providers;

import com.magicbili.islandnpc.IslandNpcPlugin;
import com.magicbili.islandnpc.api.IslandProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;

import java.util.Optional;
import java.util.UUID;

/**
 * BentoBox 岛屿提供者实现
 * 
 * @author magicbili
 */
public class BentoBoxProvider implements IslandProvider {
    
    private final IslandNpcPlugin plugin;
    private BentoBoxListener listener;
    private final BentoBox bentoBox;
    private final IslandsManager islandsManager;
    
    public BentoBoxProvider(IslandNpcPlugin plugin) {
        this.plugin = plugin;
        this.bentoBox = BentoBox.getInstance();
        this.islandsManager = BentoBox.getInstance().getIslands();
        // 延迟初始化 listener，因为此时 NpcProvider 可能还未创建
        // listener 会在 getEventListener() 被调用时初始化
    }
    
    @Override
    public String getProviderName() {
        return "BentoBox";
    }
    
    @Override
    public boolean hasIsland(Player player) {
        // BentoBox 支持多个游戏模式，这里获取玩家在任何世界的岛屿
        return islandsManager.hasIsland(player.getWorld(), player.getUniqueId());
    }
    
    @Override
    public UUID getIslandUUID(Player player) {
        Island island = islandsManager.getIsland(player.getWorld(), player.getUniqueId());
        return island != null ? UUID.fromString(island.getUniqueId()) : null;
    }
    
    @Override
    public Location getIslandCenter(UUID islandUUID) {
        Optional<Island> islandOpt = islandsManager.getIslandById(islandUUID.toString());
        if (!islandOpt.isPresent()) {
            return null;
        }
        
        Island island = islandOpt.get();
        // BentoBox 使用 getProtectionCenter() 获取岛屿保护区中心
        // 如果没有设置，则使用 getCenter()
        Location center = island.getProtectionCenter();
        if (center == null) {
            center = island.getCenter();
        }
        return center;
    }
    
    @Override
    public UUID getIslandOwner(UUID islandUUID) {
        Optional<Island> islandOpt = islandsManager.getIslandById(islandUUID.toString());
        if (!islandOpt.isPresent()) {
            return null;
        }
        
        Island island = islandOpt.get();
        return island.getOwner();
    }
    
    @Override
    public String getIslandOwnerName(UUID islandUUID) {
        UUID ownerUUID = getIslandOwner(islandUUID);
        if (ownerUUID == null) {
            return null;
        }
        
        // 尝试从在线玩家获取名称
        Player player = Bukkit.getPlayer(ownerUUID);
        if (player != null) {
            return player.getName();
        }
        
        // 从离线玩家获取名称
        return Bukkit.getOfflinePlayer(ownerUUID).getName();
    }
    
    @Override
    public boolean islandExists(UUID islandUUID) {
        return islandsManager.getIslandById(islandUUID.toString()).isPresent();
    }
    
    @Override
    public Listener getEventListener() {
        // 延迟初始化：确保 NpcProvider 已经创建
        if (listener == null) {
            listener = new BentoBoxListener(plugin, plugin.getNpcProvider());
            plugin.getLogger().info("已初始化 BentoBox 事件监听器");
        }
        return listener;
    }
    
    /**
     * 获取 BentoBox 实例
     * @return BentoBox 实例
     */
    public BentoBox getBentoBox() {
        return bentoBox;
    }
    
    /**
     * 获取岛屿管理器
     * @return 岛屿管理器
     */
    public IslandsManager getIslandsManager() {
        return islandsManager;
    }
}
