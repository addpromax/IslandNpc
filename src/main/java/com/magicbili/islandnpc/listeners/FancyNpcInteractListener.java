package com.magicbili.islandnpc.listeners;

import com.magicbili.islandnpc.IslandNpcPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class FancyNpcInteractListener implements Listener {

    private final IslandNpcPlugin plugin;

    public FancyNpcInteractListener(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNpcInteract(NpcInteractEvent event) {
        Npc npc = event.getNpc();
        Player player = event.getPlayer();

        // 检查是否是我们管理的岛屿NPC
        UUID islandUUID = plugin.getFancyNpcManager().getIslandUUIDFromNpc(npc);
        if (islandUUID == null) {
            return; // 不是岛屿NPC，忽略
        }

        // FancyNPC 会自动处理对话框打开（通过 action）
        // 这里我们只需要记录日志或执行其他自定义逻辑
        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 与岛屿NPC交互: " + islandUUID);
        }
    }
}
