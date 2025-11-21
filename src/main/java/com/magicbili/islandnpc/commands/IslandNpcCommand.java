package com.magicbili.islandnpc.commands;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.magicbili.islandnpc.IslandNpcPlugin;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class IslandNpcCommand implements CommandExecutor, TabCompleter {

    private final IslandNpcPlugin plugin;

    public IslandNpcCommand(IslandNpcPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hide":
                return handleHide(sender);
            case "show":
                return handleShow(sender);
            case "move":
                return handleMove(sender);
            case "fixall":
                return handleFixAll(sender);
            case "reload":
                return handleReload(sender);
            case "create":
                return handleCreate(sender);
            case "delete":
                return handleDelete(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                return true;
        }
    }

    private boolean handleHide(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.hide")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-island"));
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        if (plugin.getNpcManager().isNpcHidden(islandUUID)) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-already-hidden"));
            return true;
        }

        plugin.getNpcManager().hideNpc(islandUUID);
        player.sendMessage(plugin.getConfigManager().getMessage("npc-hidden"));
        return true;
    }

    private boolean handleShow(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.show")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-island"));
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        if (!plugin.getNpcManager().isNpcHidden(islandUUID)) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-already-visible"));
            return true;
        }

        plugin.getNpcManager().showNpc(islandUUID);
        player.sendMessage(plugin.getConfigManager().getMessage("npc-shown"));
        return true;
    }

    private boolean handleMove(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.move")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-island"));
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        NPC npc = plugin.getNpcManager().getIslandNpc(islandUUID);
        
        if (npc == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-not-found"));
            return true;
        }

        Location newLocation = player.getLocation();
        plugin.getNpcManager().moveNpc(islandUUID, newLocation);
        player.sendMessage(plugin.getConfigManager().getMessage("npc-moved"));
        return true;
    }

    private boolean handleCreate(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("islandnpc.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-island"));
            return true;
        }

        NPC npc = plugin.getNpcManager().createIslandNpc(island);
        if (npc != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-created"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-create-failed"));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("islandnpc.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-island"));
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        plugin.getNpcManager().deleteNpc(islandUUID);
        player.sendMessage(plugin.getConfigManager().getMessage("npc-deleted"));
        return true;
    }

    private boolean handleFixAll(CommandSender sender) {
        if (!sender.hasPermission("islandnpc.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("fixall-checking"));
        
        int fixed = 0;
        int total = 0;
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(onlinePlayer);
            if (superiorPlayer == null) continue;
            
            Island island = superiorPlayer.getIsland();
            if (island == null) continue;
            
            total++;
            UUID islandUUID = island.getUniqueId();
            NPC existingNpc = plugin.getNpcManager().getIslandNpc(islandUUID);
            
            // 检查NPC是否存在或是否有效
            if (existingNpc == null || !existingNpc.isSpawned()) {
                // 删除旧的无效记录
                if (existingNpc != null) {
                    plugin.getNpcManager().deleteNpc(islandUUID);
                }
                
                // 重新创建NPC
                NPC newNpc = plugin.getNpcManager().createIslandNpc(island);
                if (newNpc != null) {
                    fixed++;
                    plugin.getLogger().info("Fixed NPC for island: " + islandUUID + " (Owner: " + onlinePlayer.getName() + ")");
                }
            }
        }
        
        sender.sendMessage(plugin.getConfigManager().getMessage("fixall-complete", "total", String.valueOf(total), "fixed", String.valueOf(fixed)));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("islandnpc.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("help-header"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-hide"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-show"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-move"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-fixall"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-create"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-delete"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-reload"));
        sender.sendMessage(plugin.getConfigManager().getMessage("help-footer"));
    }

    private Island getPlayerIsland(Player player) {
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
        return superiorPlayer.getIsland();
    }

    private boolean hasPermission(Player player, String permission) {
        if (plugin.getConfigManager().isDefaultPermissions()) {
            return true;
        }
        return player.hasPermission(permission);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("hide", "show", "move", "fixall", "create", "delete", "reload", "help");
            String input = args[0].toLowerCase();
            
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(input)) {
                    completions.add(subCmd);
                }
            }
        }

        return completions;
    }
}
