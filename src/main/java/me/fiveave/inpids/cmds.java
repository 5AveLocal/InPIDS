package me.fiveave.inpids;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.pidsupdate.getLeftbf;

/**
 * Command class
 */
class cmds implements CommandExecutor, TabCompleter, Listener {

    /**
     * @param sender Command sender
     * @param x      Error message to be sent to command sender
     */
    private static void playerErrorMsg(CommandSender sender, String x) {
        sender.sendMessage(INPIDS_HEAD + ChatColor.RED + x);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!sender.isOp()) {
                playerErrorMsg(sender, "You have no permission!");
                return true;
            }
            if (!(sender instanceof Player p)) {
                playerErrorMsg(sender, "This is a player-only command!");
                return true;
            }
            if (args.length > 0) {
                //noinspection SwitchStatementWithTooFewBranches
                switch (args[0].toLowerCase()) {
                    case "setpids":
                        if (args.length < 5) {
                            playerErrorMsg(sender, "Correct usage: /inpids setpids <station> <platform> <style> <pidsno>");
                            return true;
                        }
                        // setpids <station> <platform> <style> <pidsno>
                        // Argument list
                        String sta = args[1];
                        int plat = Integer.parseInt(args[2]);
                        String style = args[3];
                        int pidsno = Integer.parseInt(args[4]);
                        // Get sign
                        Block b = p.getTargetBlock(Collections.singleton(Material.AIR), 5);
                        if (b.getBlockData() instanceof WallSign sign1) {
                            BlockFace bf = sign1.getFacing();
                            BlockFace leftbf = getLeftbf(bf);
                            ArrayList<Location> loclist = new ArrayList<>();
                            // Only check if style is not "null"
                            if (!style.equals("null")) {
                                // Get style height and width, check if style can be used
                                stylerec sr = stylemap.get(style);
                                int width = sr.getWidth();
                                int height = sr.getHeight();
                                for (int h = 0; h < height; h++) {
                                    for (int w = 0; w < width; w++) {
                                        Block b2 = b.getRelative(leftbf, w).getRelative(BlockFace.DOWN, h);
                                        loclist.add(b2.getLocation());
                                        // If fail then end
                                        if (!(b2.getState() instanceof Sign)) {
                                            playerErrorMsg(sender, String.format("The sign is not of %sheight = %d%s and %swidth = %d%s of the %sstyle %s%s.", ChatColor.WHITE, height, ChatColor.RED, ChatColor.WHITE, width, ChatColor.RED, ChatColor.WHITE, style, ChatColor.RED));
                                            return true;
                                        }
                                    }
                                }
                            }
                            // Set arguments
                            String pidspath = sta + "." + plat + ".locations." + pidsno;
                            String locstr = ChatColor.WHITE + pidspath + (style.equals("null") ? "" : ChatColor.GREEN + " with style " + ChatColor.WHITE + style) + ChatColor.GREEN + "." + ChatColor.GRAY + " (" + b.getLocation().getBlockX() + " " + b.getLocation().getBlockY() + " " + b.getLocation().getBlockZ() + ")";
                            if (!style.equals("null")) {
                                for (int i = 0; i < loclist.size(); i++) {
                                    stapidslist.dataconfig.set(pidspath + ".pos." + i, loclist.get(i));
                                }
                                stapidslist.dataconfig.set(pidspath + ".style", style);
                                sender.sendMessage(INPIDS_HEAD + ChatColor.GREEN + "PIDS set in " + locstr);
                            } else {
                                // If style is "null" then remove PIDS from stapidslist
                                stapidslist.dataconfig.set(pidspath, null);
                                sender.sendMessage(INPIDS_HEAD + ChatColor.GREEN + "PIDS removed in " + locstr);
                            }
                            stapidslist.save();
                        } else {
                            playerErrorMsg(sender, "Please select a sign!");
                            return true;
                        }
                        break;
                }
            } else {
                playerErrorMsg(sender, "Wrong arguments!");
            }
        } catch (Exception e) {
            errorLog(e);
        }
        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ta = new ArrayList<>();
        List<String> result = new ArrayList<>();
        int arglength = args.length;
        switch (arglength) {
            case 1:
                ta.add("setpids");
                break;
            case 3, 5:
                if (args[0].equalsIgnoreCase("setpids")) {
                    for (int i = 0; i < 10; i++) {
                        result.add(String.valueOf(i));
                    }
                }
                break;
            default:
                // Stop spamming player names
                result.add("");
                return result;
        }
        ta.forEach(a -> {
            if (a.toLowerCase().startsWith(args[arglength - 1].toLowerCase())) {
                result.add(a);
            }
        });
        return result;
    }
}