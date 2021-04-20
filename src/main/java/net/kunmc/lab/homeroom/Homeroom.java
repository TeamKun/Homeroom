package net.kunmc.lab.homeroom;

import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Homeroom extends JavaPlugin {
    public static Logger LOGGER;
    private DiscordLogic logic;
    private PlayerBinding binding;

    private boolean homeroomEnabled;
    private Location homeroomLocation;
    private double homeroomRadius;
    private Collection<Player> homeroomPlayers;

    @Override
    public void onEnable() {
        LOGGER = getLogger();

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        logic = new DiscordLogic(
                config.getString("discord.token"),
                config.getLong("discord.guildId"),
                config.getLong("discord.voiceChannelId"),
                config.getBoolean("discord.enableUnmuteOnLeave")
        );

        homeroomEnabled = config.getBoolean("homeroom.enabled");
        homeroomLocation = config.getLocation("homeroom.location");
        homeroomRadius = config.getDouble("homeroom.radius", 1);

        try {
            logic.init();
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Discord", e);
            setEnabled(false);
            return;
        }

        binding = new PlayerBinding();
        if (!binding.load(new File(getDataFolder(), "binding.csv"))) {
            LOGGER.log(Level.SEVERE, "Failed to load binding file");
            setEnabled(false);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!homeroomEnabled || homeroomLocation == null)
                    return;

                Collection<Player> before = homeroomPlayers == null || homeroomPlayers.isEmpty()
                        ? Collections.emptyList()
                        : homeroomPlayers;
                Collection<Player> players = homeroomLocation.getNearbyPlayers(homeroomRadius);
                players.stream()
                        .filter(p -> !before.contains(p))
                        .map(p -> binding.getDiscordId(p.getUniqueId(), p.getName()))
                        .filter(Objects::nonNull)
                        .forEach(p -> logic.setMute(p, false));
                before.stream()
                        .filter(p -> !players.contains(p))
                        .map(p -> binding.getDiscordId(p.getUniqueId(), p.getName()))
                        .filter(Objects::nonNull)
                        .forEach(p -> logic.setMute(p, true));
                homeroomPlayers = players;
            }
        }.runTaskTimer(this, 0, 20);
    }

    @Override
    public void onDisable() {
        logic.shutdown();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (command.getName()) {
            case "vc":
                List<Player> players = Bukkit.selectEntities(sender, args[0]).stream()
                        .filter(Player.class::isInstance)
                        .map(Player.class::cast)
                        .collect(Collectors.toList());

                if (players.isEmpty()) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.RED + "プレイヤーが見つかりません");
                    return true;
                }

                List<Long> discords = players.stream()
                        .map(p -> binding.getDiscordId(p.getUniqueId(), p.getName()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (discords.isEmpty()) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.RED + "Discord IDが見つかりません");
                    return true;
                }

                try {
                    switch (args[1]) {
                        case "on":
                            discords.forEach(p -> logic.setMute(p, false));
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "VC ON!");
                            break;
                        case "off":
                            discords.forEach(p -> logic.setMute(p, true));
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "VC OFF!");
                            break;
                        default:
                            discords.forEach(p -> {
                                Boolean muted = logic.toggleMute(p);
                                sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "VC " + (muted ? "OFF!" : "ON!"));
                            });
                            break;
                    }
                } catch (PermissionException e) {
                    LOGGER.log(Level.SEVERE, "No permission to mute", e);
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.RED + "DiscordのBotにミュート権限がありません");
                    return true;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to set mute", e);
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.RED + "Discordエラー");
                    return true;
                }
                break;

            case "homeroom":
                FileConfiguration config = getConfig();
                switch (args[0]) {
                    case "on":
                        config.set("homeroom.enabled", homeroomEnabled = true);
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "帰りの会を有効にしました");
                        break;
                    case "off":
                        config.set("homeroom.enabled", homeroomEnabled = false);
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "帰りの会を無効にしました");
                        break;
                    case "set":
                        if (sender instanceof Player)
                            homeroomLocation = ((Player) sender).getLocation();
                        else if (sender instanceof BlockCommandSender)
                            homeroomLocation = ((BlockCommandSender) sender).getBlock().getLocation();
                        config.set("homeroom.location", homeroomLocation);
                        if (args.length >= 2) {
                            homeroomRadius = NumberUtils.toDouble(args[1], 1);
                            config.set("homeroom.radius", homeroomRadius);
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "中心を設定し、半径を " + homeroomRadius + " に設定しました");
                        } else {
                            sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "中心を設定しました");
                        }
                        break;
                    case "status":
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "[かめすたプラグイン] " + ChatColor.GREEN + "帰りの会:" + homeroomEnabled + ", 位置:" + homeroomLocation + ", 半径:" + homeroomRadius);
                        break;
                    default:
                        return false;
                }
                saveConfig();
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        switch (command.getName()) {
            case "vc":
                switch (args.length) {
                    case 1:
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(e -> e.startsWith(args[0]))
                                .collect(Collectors.toList());
                    case 2:
                        return Stream.of("on", "off")
                                .filter(e -> e.startsWith(args[1]))
                                .collect(Collectors.toList());
                    default:
                        break;
                }
            case "homeroom":
                switch (args.length) {
                    case 1:
                        return Stream.of("on", "off", "set", "status")
                                .filter(e -> e.startsWith(args[0]))
                                .collect(Collectors.toList());
                    case 2:
                        if ("set".equals(args[0]))
                            return Collections.singletonList("<radius>");
                        break;
                }
        }
        return Collections.emptyList();
    }
}
