package com.magicbili.islandnpc.commands;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.magicbili.islandnpc.IslandNpcPlugin;
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
        
        // 检查NPC提供者是否可用
        if (plugin.getNpcProvider() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC提供者未初始化，请联系管理员");
            return true;
        }
        
        if (plugin.getNpcProvider().isNpcHidden(islandUUID)) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-already-hidden"));
            return true;
        }

        plugin.getNpcProvider().hideNpc(islandUUID);
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
        
        // 检查NPC提供者是否可用
        if (plugin.getNpcProvider() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC提供者未初始化，请联系管理员");
            return true;
        }
        
        if (!plugin.getNpcProvider().isNpcHidden(islandUUID)) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-already-visible"));
            return true;
        }

        plugin.getNpcProvider().showNpc(islandUUID);
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
        
        if (plugin.getNpcProvider() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC提供者未初始化，请联系管理员");
            return true;
        }
        
        if (plugin.getNpcProvider().isNpcHidden(islandUUID)) {
            plugin.getNpcProvider().showNpc(islandUUID);
            player.sendMessage(plugin.getConfigManager().getMessage("npc-toggled-visible"));
        } else {
            plugin.getNpcProvider().hideNpc(islandUUID);
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
        
        if (plugin.getNpcProvider() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC提供者未初始化，请联系管理员");
            return true;
        }
        
        if (!plugin.getNpcProvider().hasNpc(islandUUID)) {
            player.sendMessage(plugin.getConfigManager().getMessage("npc-not-found"));
            return true;
        }
        
        plugin.getNpcProvider().moveNpc(islandUUID, newLocation);
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

        if (plugin.getNpcProvider() == null || plugin.getIslandProvider() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c提供者未初始化，请联系管理员");
            return true;
        }
        
        UUID islandUUID = island.getUniqueId();
        Location center = plugin.getIslandProvider().getIslandCenter(islandUUID);
        if (center == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c无法获取岛屿中心位置");
            return true;
        }
        
        boolean success = plugin.getNpcProvider().createNpc(islandUUID, center);
        
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
        
        if (plugin.getNpcProvider() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC提供者未初始化，请联系管理员");
            return true;
        }
        
        plugin.getNpcProvider().deleteNpc(islandUUID);
        player.sendMessage(plugin.getConfigManager().getMessage("npc-deleted"));
        return true;
    }

    private boolean handleFixAll(CommandSender sender) {
        if (!sender.hasPermission("islandnpc.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (plugin.getNpcProvider() == null || plugin.getIslandProvider() == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c提供者未初始化，请联系管理员");
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
            
            // 检查NPC是否需要修复
            if (!plugin.getNpcProvider().hasNpc(islandUUID)) {
                // NPC不存在，创建新的
                Location center = plugin.getIslandProvider().getIslandCenter(islandUUID);
                if (center != null && plugin.getNpcProvider().createNpc(islandUUID, center)) {
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
