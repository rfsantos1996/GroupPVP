package com.jabyftw.gpvp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
                    + "  PRIMARY KEY (`id`),\n"
                    + "  UNIQUE INDEX `id_UNIQUE` (`id` ASC),\n"
                    + "  UNIQUE INDEX `name_UNIQUE` (`name` ASC),\n"
                    + "  UNIQUE INDEX `owner_UNIQUE` (`owner` ASC));");
            getConn().createStatement().executeUpdate("CREATE TABLE `gpvp-players` (\n"
                    + "  `name` VARCHAR(20) NOT NULL,\n"
                    + "  `groupid` INT NOT NULL,\n"
                    + "  PRIMARY KEY (`name`),\n"
                    + "  UNIQUE INDEX `name_UNIQUE` (`name` ASC));");
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
                        ResultSet rs2 = getConn().createStatement().executeQuery("SELECT `name` FROM `gpvp-players` WHERE `groupid`=" + id + ";");
                        while (rs2.next()) {
                            pl.playerNames.put(rs.getString("name"), id);
                            players.add(rs.getString("name"));
                        }
                        pl.groups.put(id, new Group(rs.getString("name"), rs.getString("owner"), players));
                    }
                    pl.getLogger().log(Level.INFO, "Loaded all groups and players.");
                } catch (SQLException e) {
                    pl.getLogger().log(Level.WARNING, "Couldn't load groups");
                }
            }
        });
    }

    public int saveGroup(String name, String owner) {
        try {
            Statement s = getConn().createStatement();
            s.executeUpdate("INSERT INTO `gpvp-groups` (`name`, `owner`) VALUES ('" + name + "', " + owner.toLowerCase() + ");", Statement.RETURN_GENERATED_KEYS);
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
                ResultSet rs = getConn().createStatement().executeQuery("SELECT `name` FROM `gpvp-groups` WHERE `name`='" + g.getName() + "';");
                if (!rs.next()) {
                    getConn().createStatement().execute("INSERT INTO `gpvp-groups` (`name`, `owner`) VALUES ('" + g.getName() + "', " + g.getOwner() + ");");
                }
            }
            for (Map.Entry<String, Integer> set : pl.playerNames.entrySet()) {
                ResultSet rs = getConn().createStatement().executeQuery("SELECT `groupid` FROM `gpvp-players` WHERE `name`='" + set.getKey() + "';");
                if (rs.next()) {
                    if (rs.getInt("groupid") != set.getValue()) {
                        getConn().createStatement().executeUpdate("UPDATE `gpvp-players` SET `groupid`=" + set.getValue() + " WHERE `name`='" + set.getKey().toLowerCase() + "';");
                    }
                } else {
                    getConn().createStatement().execute("INSERT INTO `gpvp-players` (`name`, `groupid`) VALUES ('" + set.getKey().toLowerCase() + "', " + set.getValue() + ");");
                }
            }
            for (String s : pl.toDelete) {
                getConn().createStatement().execute("DELETE FROM `gpvp-players` WHERE `name`='" + s.toLowerCase() + "';");
                pl.toDelete.remove(s);
            }
        } catch (SQLException e) {
            pl.getLogger().log(Level.WARNING, "Couldn't save to database.");
        }
    }
}
