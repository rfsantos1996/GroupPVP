package com.jabyftw.gpvp;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 *
 * @author Rafael
 */
public class Group {

    private final String name;
    private final String owner;
    private final int id, maxSize;
    private Location base;
    private List<String> players = new ArrayList();

    Group(int id, int max, String name, String owner, Location base, List<String> players) {
        this.id = id;
        this.maxSize = max;
        this.name = name;
        this.owner = owner;
        this.base = base;
        this.players.addAll(players);
    }

    public String getName() {
        return name;
    }

    public boolean isOnPlayerList(Player player) {
        return players.contains(player.getName().toLowerCase());
    }

    public List<String> getPlayers() {
        return players;
    }

    public Location getBase() {
        return base;
    }

    public void setBase(Location loc) {
        base = loc;
    }

    public int getId() {
        return id;
    }

    public boolean isOwner(String name) {
        return owner.equalsIgnoreCase(name);
    }

    public String getOwner() {
        return owner.toLowerCase();
    }

    public void removePlayer(String name) {
        players.remove(name.toLowerCase());
    }

    public boolean addPlayer(String name) {
        if (players.size() < maxSize) {
            players.add(name.toLowerCase());
            return true;
        }
        return false;
    }
}
