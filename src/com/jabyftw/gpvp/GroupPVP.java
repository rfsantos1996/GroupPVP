package com.jabyftw.gpvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Rafael
 */
public class GroupPVP extends JavaPlugin implements Listener {
    
    private FileConfiguration config;
    public MySQL sql;
    public int maxPlayers;
    public List<String> toDelete = new ArrayList();
    public Map<String, Integer> playerNames = new HashMap();
    public Map<Integer, Group> groups = new HashMap();
    public Map<Player, Group> players = new HashMap();
    
    @Override
    public void onEnable() {
        config = getConfig();
        config.addDefault("config.maxPlayersPerGroup", 5);
        config.addDefault("config.saveDelayInTicks", 36000);
        config.addDefault("config.mysql.user", "root");
        config.addDefault("config.mysql.password", "root");
        config.addDefault("config.mysql.url", "jdbc:mysql://localhost:3306/database");
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
        maxPlayers = config.getInt("config.maxPlayersPerGroup");
        sql = new MySQL(this, config.getString("config.mysql.user"), config.getString("config.mysql.password"), config.getString("config.mysql.url"));
        sql.createTables();
        sql.loadGroups();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginCommand("gpvp").setExecutor(new GPVPExecutor(this));
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, new BukkitRunnable() {
            
            @Override
            public void run() {
                sql.saveAllAsync();
            }
        }, config.getInt("config.saveDelayInTicks"), config.getInt("config.saveDelayInTicks"));
    }
    
    @Override
    public void onDisable() {
        sql.saveAllSync();
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (playerNames.containsKey(p.getName().toLowerCase())) {
            Group g = groups.get(playerNames.get(p.getName().toLowerCase()));
            if (g != null) {
                if (g.isOnPlayerList(p)) {
                    players.put(p, g);
                } else {
                    p.sendMessage("Voce foi expulso.");
                    playerNames.remove(p.getName().toLowerCase());
                    toDelete.add(p.getName().toLowerCase());
                }
            } else {
                p.sendMessage("O grupo foi deletado.");
                playerNames.remove(p.getName().toLowerCase());
                toDelete.add(p.getName().toLowerCase());
            }
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        checkDisconnect(e.getPlayer());
    }
    
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        checkDisconnect(e.getPlayer());
    }
    
    private void checkDisconnect(Player player) {
        if (players.containsKey(player)) {
            players.remove(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {
            Player damaged = (Player) e.getEntity();
            Player damager = null;
            if (e.getDamager() instanceof Player) {
                damager = (Player) e.getDamager();
            } else if (e.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) e.getDamager();
                if (proj.getShooter() instanceof Player) {
                    damager = (Player) proj.getShooter();
                }
            }
            if (damager != null && players.containsKey(damaged) && players.containsKey(damager)) {
                if (players.get(damaged).equals(players.get(damager))) {
                    damager.sendMessage("Nao bata no seu aliado!");
                    e.setCancelled(true);
                }
            }
        }
    }
}
