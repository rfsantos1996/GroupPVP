package com.jabyftw.gpvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Rafael
 */
public class GroupPVP extends JavaPlugin implements Listener {

    private FileConfiguration config;
    public MySQL sql;
    public boolean askOnJoin;
    public int maxPlayers;
    public List<String> toPDelete = new ArrayList();
    public List<Integer> toGDelete = new ArrayList();
    public Map<String, Integer> playerNames = new HashMap();
    public Map<Integer, Group> groups = new HashMap();
    public Map<Player, Group> players = new HashMap();
    public Map<Player, Group> invitations = new HashMap();

    @Override
    public void onEnable() {
        config = getConfig();
        config.addDefault("config.maxPlayersPerGroup", 5);
        config.addDefault("config.saveDelayInTicks", 36000);
        config.addDefault("config.askToCreateGroupOnJoin", true);
        config.addDefault("config.mysql.user", "root");
        config.addDefault("config.mysql.password", "root");
        config.addDefault("config.mysql.url", "jdbc:mysql://localhost:3306/database");
        //config.addDefault("lang.", "&");
        config.addDefault("lang.alreadyOnAGroup", "&cYou are already on a group.");
        config.addDefault("lang.alreadyOnAnotherGroup", "&cPlayer is on other group.");
        config.addDefault("lang.groupCreated", "&6Group created!");
        config.addDefault("lang.couldntCreateGroup", "&4Couldnt create group.");
        config.addDefault("lang.playerInvited", "&6Player invited.");
        config.addDefault("lang.youWereInvited", "&6You were invited for group &e%gname&6 by &e%owner&6, use &e/gpvp accept&6 to participate");
        config.addDefault("lang.playerKicked", "&cPlayer kicked.");
        config.addDefault("lang.youWereKicked", "&cYou have been kicked from the group by &4%owner&c.");
        config.addDefault("lang.youJoinedGroup", "&6You joined group &e%gname&6.");
        config.addDefault("lang.groupIsFull", "&4Sorry, the group is full. &cContact the group owner.");
        config.addDefault("lang.noInvitations", "&cYou dont have any invitations.");
        config.addDefault("lang.playerNotFound", "&cPlayer not found.");
        config.addDefault("lang.noPermission", "&cNo permission.");
        config.addDefault("lang.youArentOnAnyGroup", "&4You dont have any group.");
        config.addDefault("lang.groupNotFound", "&cGroup not found. :/");
        config.addDefault("lang.dontPunchYourAlly", "&cDont punch your ally.");
        config.addDefault("lang.changedBaseLocation", "&6Changed base location.");
        config.addDefault("lang.groupDeleted", "&4Group deleted.");
        config.addDefault("lang.yourGroupWasDeleted", "&cSorry, your group was deleted.");
        config.addDefault("lang.questionOnJoin", "&6You dont have a group, you can create one using &e/gpvp&6.");
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
        maxPlayers = config.getInt("config.maxPlayersPerGroup");
        askOnJoin = config.getBoolean("config.askToCreateGroupOnJoin");
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

    public String getLang(String path) {
        return config.getString("lang." + path).replaceAll("&", "§");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (players.containsKey(e.getPlayer())) {
            Location base = players.get(e.getPlayer()).getBase();
            if (e.getPlayer().getLocation().getWorld().equals(base.getWorld())) {
                e.getPlayer().setCompassTarget(base);
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (players.containsKey(e.getPlayer())) {
            Location base = players.get(e.getPlayer()).getBase();
            if (e.getPlayer().getLocation().getWorld().equals(base.getWorld())) {
                e.getPlayer().setCompassTarget(base);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (playerNames.containsKey(p.getName().toLowerCase())) {
            int gId = playerNames.get(p.getName().toLowerCase());
            Group g = groups.get(gId);
            if (g != null) {
                if (g.isOnPlayerList(p)) {
                    players.put(p, g);
                    if (p.getLocation().getWorld().equals(g.getBase().getWorld())) {
                        p.setCompassTarget(g.getBase());
                    }
                } else {
                    p.sendMessage(getLang("youWereKicked").replaceAll("%owner", g.getOwner()));
                    playerNames.remove(p.getName().toLowerCase());
                    toPDelete.add(p.getName().toLowerCase());
                }
            } else {
                p.sendMessage(getLang("groupNotFound"));
                toGDelete.add(gId);
                playerNames.remove(p.getName().toLowerCase());
            }
        } else {
            if(askOnJoin) {
                p.sendMessage(getLang("questionOnJoin"));
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
                    damager.sendMessage(getLang("dontPunchYourAlly"));
                    e.setCancelled(true);
                }
            }
        }
    }
}
