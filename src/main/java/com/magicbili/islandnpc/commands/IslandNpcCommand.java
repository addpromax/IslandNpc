package com.magicbili.islandnpc.commands;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.magicbili.islandnpc.IslandNpcPlugin;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
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
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /islandnpc help");
                return true;
        }
    }

    private boolean handleHide(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.hide")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island!");
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        if (plugin.getNpcManager().isNpcHidden(islandUUID)) {
            player.sendMessage(ChatColor.YELLOW + "Your island NPC is already hidden!");
            return true;
        }

        plugin.getNpcManager().hideNpc(islandUUID);
        player.sendMessage(ChatColor.GREEN + "Island NPC has been hidden!");
        return true;
    }

    private boolean handleShow(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.show")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island!");
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        if (!plugin.getNpcManager().isNpcHidden(islandUUID)) {
            player.sendMessage(ChatColor.YELLOW + "Your island NPC is already visible!");
            return true;
        }

        plugin.getNpcManager().showNpc(islandUUID);
        player.sendMessage(ChatColor.GREEN + "Island NPC is now visible!");
        return true;
    }

    private boolean handleMove(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "islandnpc.move")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island!");
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        NPC npc = plugin.getNpcManager().getIslandNpc(islandUUID);
        
        if (npc == null) {
            player.sendMessage(ChatColor.RED + "No NPC found for your island!");
            return true;
        }

        Location newLocation = player.getLocation();
        plugin.getNpcManager().moveNpc(islandUUID, newLocation);
        player.sendMessage(ChatColor.GREEN + "Island NPC has been moved to your location!");
        return true;
    }

    private boolean handleCreate(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("islandnpc.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island!");
            return true;
        }

        NPC npc = plugin.getNpcManager().createIslandNpc(island);
        if (npc != null) {
            player.sendMessage(ChatColor.GREEN + "Island NPC has been created!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to create island NPC!");
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("islandnpc.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Island island = getPlayerIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island!");
            return true;
        }

        UUID islandUUID = island.getUniqueId();
        plugin.getNpcManager().deleteNpc(islandUUID);
        player.sendMessage(ChatColor.GREEN + "Island NPC has been deleted!");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("islandnpc.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage(ChatColor.GREEN + "IslandNpc configuration has been reloaded!");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== IslandNpc Commands ==========");
        sender.sendMessage(ChatColor.YELLOW + "/islandnpc hide" + ChatColor.GRAY + " - Hide your island NPC");
        sender.sendMessage(ChatColor.YELLOW + "/islandnpc show" + ChatColor.GRAY + " - Show your island NPC");
        sender.sendMessage(ChatColor.YELLOW + "/islandnpc move" + ChatColor.GRAY + " - Move NPC to your location");
        sender.sendMessage(ChatColor.YELLOW + "/islandnpc create" + ChatColor.GRAY + " - Create island NPC (Admin)");
        sender.sendMessage(ChatColor.YELLOW + "/islandnpc delete" + ChatColor.GRAY + " - Delete island NPC (Admin)");
        sender.sendMessage(ChatColor.YELLOW + "/islandnpc reload" + ChatColor.GRAY + " - Reload config & update NPCs (Admin)");
        sender.sendMessage(ChatColor.GOLD + "======================================");
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
            List<String> subCommands = Arrays.asList("hide", "show", "move", "create", "delete", "reload", "help");
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
