package com.jabyftw.gpvp;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Rafael
 */
public class GPVPExecutor implements CommandExecutor {

    private final GroupPVP pl;

    public GPVPExecutor(GroupPVP pl) {
        this.pl = pl;
    }

    @Override // /gpvp (create/invite/kick) (name)
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (sender instanceof Player) {
                if (sender.hasPermission("gpvp.use")) {
                    Player p = (Player) sender;
                    if (args.length > 1) {
                        if (args[0].startsWith("c")) { // create
                            if (pl.players.containsKey(p)) {
                                sender.sendMessage(pl.getLang("alreadyOnAGroup"));
                                return true;
                            } else {
                                String owner = sender.getName().toLowerCase();
                                int id = pl.sql.saveGroup(args[1], owner, p.getLocation());
                                if (id > 0) {
                                    Group g = new Group(id, pl.maxPlayers, args[1], owner, p.getLocation(), Arrays.asList(sender.getName().toLowerCase()));
                                    pl.groups.put(id, g);
                                    pl.playerNames.put(owner, id);
                                    pl.players.put(p, g);
                                    sender.sendMessage(pl.getLang("groupCreated"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("couldntCreateGroup"));
                                    return true;
                                }
                            }
                        } else if (args[0].startsWith("i")) { // invite
                            if (pl.players.containsKey(p)) {
                                String invitedName = args[1];
                                Player invited = pl.getServer().getPlayer(invitedName);
                                Group g = pl.players.get(p);
                                if (g.getOwner().equalsIgnoreCase(sender.getName())) {
                                    if (invited != null) {
                                        if (pl.players.containsKey(invited)) {
                                            sender.sendMessage(pl.getLang("alreadyOnAnotherGroup"));
                                            return true;
                                        } else {
                                            pl.invitations.put(invited, g);
                                            sender.sendMessage(pl.getLang("playerInvited"));
                                            invited.sendMessage(pl.getLang("youWereInvited").replaceAll("%gname", g.getName()).replaceAll("%owner", g.getOwner()));
                                            return true;
                                        }
                                    } else {
                                        sender.sendMessage(pl.getLang("playerNotFound"));
                                        return true;
                                    }
                                } else {
                                    sender.sendMessage(pl.getLang("noPermission"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("youArentOnAnyGroup"));
                                return true;
                            }
                        } else { // kick
                            if (pl.players.containsKey(p)) {
                                String kickedName = args[1];
                                Player kicked = pl.getServer().getPlayer(kickedName);
                                Group g = pl.players.get(p);
                                if (g.isOwner(sender.getName())) {
                                    if (kicked != null) {
                                        if (pl.players.containsKey(kicked) && pl.players.get(kicked).equals(g)) {
                                            pl.players.remove(kicked);
                                            pl.playerNames.remove(kicked.getName().toLowerCase());
                                            pl.toPDelete.add(kicked.getName().toLowerCase());
                                            g.removePlayer(kicked.getName().toLowerCase());
                                            sender.sendMessage(pl.getLang("playerKicked"));
                                            kicked.sendMessage(pl.getLang("youWereKicked").replaceAll("%owner", g.getOwner()));
                                            return true;
                                        } else {
                                            sender.sendMessage(pl.getLang("alreadyOnAnotherGroup"));
                                            return true;
                                        }
                                    } else {
                                        sender.sendMessage(pl.getLang("playerNotFound"));
                                        return true;
                                    }
                                } else {
                                    sender.sendMessage(pl.getLang("noPermission"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("youArentOnAnyGroup"));
                                return true;
                            }
                        }
                    } else { // accept
                        if (args[0].startsWith("a")) {
                            if (pl.invitations.containsKey(p)) {
                                Group g = pl.invitations.get(p);
                                if (g.addPlayer(p.getName())) {
                                    pl.playerNames.put(p.getName().toLowerCase(), g.getId());
                                    pl.players.put(p, g);
                                    pl.invitations.remove(p);
                                    sender.sendMessage(pl.getLang("youJoinedGroup").replaceAll("%gname", g.getName()));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("groupIsFull"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("noInvitations"));
                                return true;
                            }
                        } else if (args[0].startsWith("d")) { // deleted
                            if (pl.players.containsKey(p)) {
                                Group g = pl.players.get(p);
                                if (g.isOwner(p.getName().toLowerCase())) {
                                    int id = g.getId();
                                    for (String s : g.getPlayers()) {
                                        pl.playerNames.remove(s);
                                        pl.toPDelete.add(s);
                                    }
                                    for (Map.Entry<Player, Group> set : pl.players.entrySet()) {
                                        if (set.getValue().equals(g)) {
                                            set.getKey().sendMessage(pl.getLang("yourGroupWasDeleted"));
                                            pl.players.remove(set.getKey());
                                        }
                                    }
                                    pl.groups.remove(id);
                                    pl.toGDelete.add(id);
                                    sender.sendMessage(pl.getLang("groupDeleted"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("noPermission"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("youArentOnAnyGroup"));
                                return true;
                            }
                        } else { // set base
                            if (pl.players.containsKey(p)) {
                                Group g = pl.players.get(p);
                                if (g.isOwner(p.getName().toLowerCase())) {
                                    g.setBase(p.getLocation());
                                    sender.sendMessage(pl.getLang("changedBaseLocation"));
                                    return true;
                                } else {
                                    sender.sendMessage(pl.getLang("noPermission"));
                                    return true;
                                }
                            } else {
                                sender.sendMessage(pl.getLang("youArentOnAnyGroup"));
                                return true;
                            }
                        }
                    }
                } else {
                    sender.sendMessage(pl.getLang("noPermission"));
                    return true;
                }
            } else {
                sender.sendMessage(pl.getLang("noPermission"));
                return true;
            }
        } else {
            return false;
        }
    }
}
