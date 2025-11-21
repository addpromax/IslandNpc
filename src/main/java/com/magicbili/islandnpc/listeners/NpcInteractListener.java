package com.magicbili.islandnpc.listeners;

import com.fancyinnovations.fancydialogs.api.Dialog;
import com.fancyinnovations.fancydialogs.api.FancyDialogs;
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
                player.sendMessage(plugin.getConfigManager().getMessage("dialog-not-configured"));
            }
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-clicked"));
        }
    }

    private void openFancyDialog(Player player, String dialogId) {
        try {
            FancyDialogs fancyDialogs = FancyDialogs.get();
            if (fancyDialogs == null) {
                plugin.getLogger().warning("FancyDialogs plugin not loaded!");
                return;
            }
            
            Dialog dialog = fancyDialogs.getDialogRegistry().get(dialogId);
            if (dialog == null) {
                plugin.getLogger().warning("Dialog not found: " + dialogId);
                player.sendMessage(plugin.getConfigManager().getMessage("dialog-not-found", "id", dialogId));
                return;
            }
            
            dialog.open(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open FancyDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
