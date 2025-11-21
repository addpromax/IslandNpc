package com.magicbili.islandnpc.listeners;

import com.magicbili.islandnpc.IslandNpcPlugin;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class NpcInteractListener implements Listener {

    private final IslandNpcPlugin plugin;

    public NpcInteractListener(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        Player player = event.getClicker();

        UUID islandUUID = plugin.getNpcManager().getIslandUUIDFromNpc(npc);
        if (islandUUID == null) return;

        if (Bukkit.getPluginManager().getPlugin("FancyDialogs") != null) {
            String dialogId = npc.data().get("dialogId");
            if (dialogId != null && !dialogId.isEmpty()) {
                openFancyDialog(player, dialogId);
            } else {
                player.sendMessage("§cDialog ID not configured!");
            }
        } else {
            player.sendMessage("§aYou clicked the island NPC!");
        }
    }

    private void openFancyDialog(Player player, String dialogId) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                "fancydialogs open " + dialogId + " " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open FancyDialog: " + e.getMessage());
        }
    }
}
