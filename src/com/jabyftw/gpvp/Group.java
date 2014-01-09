package com.jabyftw.gpvp;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

/**
 *
 * @author Rafael
 */
public class Group {
    
    private final String name;
    private final String owner;
    private List<String> players = new ArrayList();
    
    Group(String name, String owner, List<String> players) {
        this.name = name;
        this.owner = owner;
        this.players.addAll(players);
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isOnPlayerList(Player player) {
        return players.contains(player.getName().toLowerCase());
    }
    
    public boolean isOwner(String name) {
        return owner.equalsIgnoreCase(name);
    }
    
    public String getOwner() {
        return owner.toLowerCase();
    }
}
