package com.jabyftw.gpvp;

import java.util.Arrays;
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
        if (args.length < 2) {
            return false;
        } else {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args[0].startsWith("c")) {
                    if (pl.players.containsKey(p)) {
                        sender.sendMessage("Voce ja esta em um grupo.");
                        return true;
                    } else {
                        String owner = sender.getName().toLowerCase();
                        int id = pl.sql.saveGroup(args[1], owner);
                        if (id > 0) {
                            Group g = new Group(args[1], owner, Arrays.asList(sender.getName().toLowerCase()));
                            pl.groups.put(id, g);
                            pl.playerNames.put(owner, id);
                            pl.players.put(p, g);
                            sender.sendMessage("Grupo criado.");
                            return true;
                        } else {
                            sender.sendMessage("Nao foi possivel criar o grupo.");
                            return true;
                        }
                    }
                } else if (args[0].startsWith("i")) {
                    if (pl.players.containsKey(p)) {

                    } else {
                        sender.sendMessage("Voce nao esta em nenhum grupo.");
                        return true;
                    }
                } else {

                }
            } else {
                sender.sendMessage("Apenas em jogo.");
                return true;
            }
        }
        return false;
    }
}
