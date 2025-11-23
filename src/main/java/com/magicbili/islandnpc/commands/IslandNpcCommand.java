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
            case "toggle":
                return handleToggle(sender);
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
        
        boolean isHidden = plugin.getNpcManager() != null ? 
            plugin.getNpcManager().isNpcHidden(islandUUID) : 
            plugin.getFancyNpcManager().isNpcHidden(islandUUID);
            
        if (isHidden) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-already-hidden"));
            return true;
        }

        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().hideNpc(islandUUID);
        } else {
            plugin.getFancyNpcManager().hideNpc(islandUUID);
        }
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
        
        boolean isHidden = plugin.getNpcManager() != null ? 
            plugin.getNpcManager().isNpcHidden(islandUUID) : 
            plugin.getFancyNpcManager().isNpcHidden(islandUUID);
            
        if (!isHidden) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-already-visible"));
            return true;
        }

        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().showNpc(islandUUID);
        } else {
            plugin.getFancyNpcManager().showNpc(islandUUID);
        }
        player.sendMessage(plugin.getConfigManager().getMessage("npc-shown"));
        return true;
    }

    private boolean handleToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.toggle")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-island"));
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        
        boolean isHidden = plugin.getNpcManager() != null ? 
            plugin.getNpcManager().isNpcHidden(islandUUID) : 
            plugin.getFancyNpcManager().isNpcHidden(islandUUID);
        
        if (isHidden) {
            if (plugin.getNpcManager() != null) {
                plugin.getNpcManager().showNpc(islandUUID);
            } else {
                plugin.getFancyNpcManager().showNpc(islandUUID);
            }
            player.sendMessage(plugin.getConfigManager().getMessage("npc-toggled-visible"));
        } else {
            if (plugin.getNpcManager() != null) {
                plugin.getNpcManager().hideNpc(islandUUID);
            } else {
                plugin.getFancyNpcManager().hideNpc(islandUUID);
            }
            player.sendMessage(plugin.getConfigManager().getMessage("npc-toggled-hidden"));
        }
        
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
        Location newLocation = player.getLocation();
        
        if (plugin.getNpcManager() != null) {
            NPC npc = plugin.getNpcManager().getIslandNpc(islandUUID);
            if (npc == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("npc-not-found"));
                return true;
            }
            plugin.getNpcManager().moveNpc(islandUUID, newLocation);
        } else {
            de.oliver.fancynpcs.api.Npc npc = plugin.getFancyNpcManager().getIslandNpc(islandUUID);
            if (npc == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("npc-not-found"));
                return true;
            }
            plugin.getFancyNpcManager().moveNpc(islandUUID, newLocation);
        }
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

        boolean success = false;
        if (plugin.getNpcManager() != null) {
            NPC npc = plugin.getNpcManager().createIslandNpc(island);
            success = npc != null;
        } else {
            de.oliver.fancynpcs.api.Npc npc = plugin.getFancyNpcManager().createIslandNpc(island);
            success = npc != null;
        }
        
        if (success) {
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
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().deleteNpc(islandUUID);
        } else {
            plugin.getFancyNpcManager().deleteNpc(islandUUID);
        }
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
            
            boolean needsFix = false;
            if (plugin.getNpcManager() != null) {
                NPC existingNpc = plugin.getNpcManager().getIslandNpc(islandUUID);
                needsFix = (existingNpc == null || !existingNpc.isSpawned());
                
                if (needsFix) {
                    if (existingNpc != null) {
                        plugin.getNpcManager().deleteNpc(islandUUID);
                    }
                    NPC newNpc = plugin.getNpcManager().createIslandNpc(island);
                    if (newNpc != null) {
                        fixed++;
                        plugin.getLogger().info("Fixed NPC for island: " + islandUUID + " (Owner: " + onlinePlayer.getName() + ")");
                    }
                }
            } else {
                de.oliver.fancynpcs.api.Npc existingNpc = plugin.getFancyNpcManager().getIslandNpc(islandUUID);
                needsFix = (existingNpc == null);
                
                if (needsFix) {
                    if (existingNpc != null) {
                        plugin.getFancyNpcManager().deleteNpc(islandUUID);
                    }
                    de.oliver.fancynpcs.api.Npc newNpc = plugin.getFancyNpcManager().createIslandNpc(island);
                    if (newNpc != null) {
                        fixed++;
                        plugin.getLogger().info("Fixed NPC for island: " + islandUUID + " (Owner: " + onlinePlayer.getName() + ")");
                    }
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
        sender.sendMessage(plugin.getConfigManager().getMessage("help-toggle"));
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
            List<String> subCommands = Arrays.asList("hide", "show", "toggle", "move", "fixall", "create", "delete", "reload", "help");
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
