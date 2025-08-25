package cz.kolt.koltcheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KoltCheat extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Long> lastMove = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("koltcheat").setExecutor(this);
        getLogger().info("KoltCheat aktivován!");
    }

    @Override
    public void onDisable() {
        getLogger().info("KoltCheat deaktivován.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isFlying() && !player.getAllowFlight()) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && from.getY() < to.getY() && player.getVelocity().getY() > 0.6) {
                kickPlayer(player, "Fly hack detekován");
            }
        }
        long now = System.currentTimeMillis();
        if (lastMove.containsKey(player.getUniqueId())) {
            long diff = now - lastMove.get(player.getUniqueId());
            if (diff < 30) {
                kickPlayer(player, "Speed hack detekován");
            }
        }
        lastMove.put(player.getUniqueId(), now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastMove.remove(event.getPlayer().getUniqueId());
    }

    private void kickPlayer(Player player, String reason) {
        String logMsg = player.getName() + " byl vyhozen: " + reason;
        player.kickPlayer(ChatColor.RED + "[KoltCheat] " + reason);

        // Zpráva OP hráčům
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()) {
                p.sendMessage(ChatColor.RED + "[KoltCheat] " + logMsg);
            }
        }

        // StaffChat příkaz (pokud existuje)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "staffchat [KoltCheat] " + player.getName() + " vyhozen: " + reason);

        logToFile(logMsg);
    }

    private void logToFile(String message) {
        try {
            File folder = new File(getDataFolder(), "logs");
            if (!folder.exists()) folder.mkdirs();
            File log = new File(folder, "cheat-log.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(log, true));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            writer.write("[" + timestamp + "] " + message);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tento příkaz mohou používat pouze hráči.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/koltcheat info - zobrazí informace");
            sender.sendMessage(ChatColor.YELLOW + "/koltcheat reload - načte znovu konfiguraci");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GREEN + "KoltCheat v1.0 - jednoduchý anti-cheat plugin");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Konfigurace znovu načtena.");
            return true;
        }

        return false;
    }
}