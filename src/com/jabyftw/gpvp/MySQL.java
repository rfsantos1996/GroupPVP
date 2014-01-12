package com.jabyftw.gpvp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class MySQL {

    private final GroupPVP pl;
    private final String user, pass, url;
    public Connection conn = null;

    public MySQL(GroupPVP pl, String username, String password, String url) {
        this.pl = pl;
        this.user = username;
        this.pass = password;
        this.url = url;
    }

    public Connection getConn() {
        if (conn != null) {
            return conn;
        }
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't connect to MySQL: {0}", e.getMessage());
        }
        return conn;
    }

    public void closeConn() {
        if (conn != null) {
            try {
                conn.close();
                conn = null;
            } catch (SQLException ex) {
                pl.getLogger().log(Level.WARNING, "Couldn't connect to MySQL: {0}", ex.getMessage());
            }
        }
    }

    public void createTables() {
        try {
            getConn().createStatement().executeUpdate("CREATE TABLE `gpvp-groups` (\n"
                    + "  `id` INT NOT NULL AUTO_INCREMENT,\n"
                    + "  `name` VARCHAR(24) NOT NULL,\n"
                    + "  `owner` VARCHAR(20) NOT NULL,\n"
                    + "  `locbase` VARCHAR(60) NOT NULL,\n"
                    + "  PRIMARY KEY (`id`),\n"
                    + "  UNIQUE INDEX `id_UNIQUE` (`id` ASC),\n"
                    + "  UNIQUE INDEX `name_UNIQUE` (`name` ASC),\n"
                    + "  UNIQUE INDEX `owner_UNIQUE` (`owner` ASC));");
            getConn().createStatement().executeUpdate("CREATE TABLE `gpvp-players` (\n"
                    + "  `name` VARCHAR(20) NOT NULL,\n"
                    + "  `groupid` INT NOT NULL,\n"
                    + "  PRIMARY KEY (`name`),\n"
                    + "  UNIQUE INDEX `name_UNIQUE` (`name` ASC));");
            if (Double.parseDouble(pl.getDescription().getVersion()) < 0.2D) {
                getConn().createStatement().executeUpdate("ALTER TABLE `gpvp-groups` \n"
                        + "ADD COLUMN `locbase` VARCHAR(60) NOT NULL AFTER `owner`;");
            }
        } catch (SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't create tables");
        }
    }

    public void loadGroups() {
        pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new BukkitRunnable() {

            @Override
            public void run() {
                try {
                    ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM `gpvp-groups`;");
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        List<String> players = new ArrayList();
                        String[] loc = rs.getString("locbase").split(";");
                        Location base = new Location(pl.getServer().getWorld(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2]), Integer.parseInt(loc[3]));
                        ResultSet rs2 = getConn().createStatement().executeQuery("SELECT `name` FROM `gpvp-players` WHERE `groupid`=" + id + ";");
                        while (rs2.next()) {
                            pl.playerNames.put(rs2.getString("name"), id);
                            players.add(rs2.getString("name"));
                        }
                        pl.groups.put(id, new Group(id, pl.maxPlayers, rs.getString("name"), rs.getString("owner"), base, players));
                    }
                    pl.getLogger().log(Level.INFO, "Loaded " + pl.groups.size() + " groups and " + pl.playerNames.size() + " players.");
                } catch (SQLException e) {
                    pl.getLogger().log(Level.WARNING, "Couldn't load groups");
                }
            }
        });
    }

    public int saveGroup(String name, String owner, Location loc) {
        String locS = loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
        try {
            Statement s = getConn().createStatement();
            s.executeUpdate("INSERT INTO `gpvp-groups` (`name`, `owner`, `locbase`) VALUES ('" + name + "', '" + owner.toLowerCase() + "', '" + locS + "');", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = s.getGeneratedKeys();
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't save group.");
        }
        return -1;
    }

    public void saveAllSync() {
        pl.getLogger().log(Level.INFO, "Saving Sync...");
        save();
        closeConn();
        pl.getLogger().log(Level.INFO, "Saved and closed!");
    }

    public void saveAllAsync() {
        pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new BukkitRunnable() {

            @Override
            public void run() {
                pl.getLogger().log(Level.INFO, "Saving Async...");
                save();
                pl.getLogger().log(Level.INFO, "Saved!");
            }
        });
    }

    private void save() {
        try {
            for (Group g : pl.groups.values()) {
                ResultSet rs = getConn().createStatement().executeQuery("SELECT `locbase` FROM `gpvp-groups` WHERE `name`='" + g.getName() + "';");
                String locS = g.getBase().getWorld().getName() + ";" + g.getBase().getBlockX() + ";" + g.getBase().getBlockY() + ";" + g.getBase().getBlockZ();
                if (rs.next()) {
                    if (rs.getString("locbase").equalsIgnoreCase(locS)) {
                        getConn().createStatement().executeUpdate("UPDATE `gpvp-groups` SET `locbase`='" + locS + "' WHERE `name`='" + g.getName() + "';");
                    }
                } else {
                    getConn().createStatement().execute("INSERT INTO `gpvp-groups` (`name`, `owner`, `locbase`) VALUES ('" + g.getName() + "', '" + g.getOwner() + "', '" + locS + "');");
                }
            }
            for (Map.Entry<String, Integer> set : pl.playerNames.entrySet()) {
                ResultSet rs = getConn().createStatement().executeQuery("SELECT `groupid` FROM `gpvp-players` WHERE `name`='" + set.getKey() + "';");
                if (rs.next()) {
                    if (rs.getInt("groupid") != set.getValue()) { // this will update the group if not the same
                        getConn().createStatement().executeUpdate("UPDATE `gpvp-players` SET `groupid`=" + set.getValue() + " WHERE `name`='" + set.getKey().toLowerCase() + "';");
                    }
                } else {
                    getConn().createStatement().execute("INSERT INTO `gpvp-players` (`name`, `groupid`) VALUES ('" + set.getKey().toLowerCase() + "', " + set.getValue() + ");");
                }
            }
            for (String s : pl.toPDelete) {
                getConn().createStatement().execute("DELETE FROM `gpvp-players` WHERE `name`='" + s.toLowerCase() + "';");
                pl.toPDelete.remove(s);
            }
            for (int i : pl.toGDelete) {
                getConn().createStatement().execute("DELETE FROM `gpvp-groups` WHERE `id`=" + i + ";");
                //getConn().createStatement().execute("DELETE FROM `gpvp-players` WHERE `groupid`=" + i + ";"); -- Safe key thing on MySQL
                pl.toGDelete.remove(i);
            }
        } catch (SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't save to database.");
        }
    }
}
