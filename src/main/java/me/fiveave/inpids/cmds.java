package me.fiveave.inpids;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
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

import static me.fiveave.inpids.pidsupdate.getLeftbf;
import static me.fiveave.inpids.main.*;

class cmds implements CommandExecutor, TabCompleter, Listener {

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
                        // setpids <station> <platform> <pidsno> <style>
                        // Argument list
                        String sta = args[1];
                        int plat = Integer.parseInt(args[2]);
                        int pidsno = Integer.parseInt(args[3]);
                        String style = args[4];
                        // Get sign
                        Block b = p.getTargetBlock(Collections.singleton(Material.AIR), 5);
                        if (b.getState() instanceof Sign) {
                            WallSign sign1 = (WallSign) b.getBlockData();
                            BlockFace bf = sign1.getFacing();
                            BlockFace leftbf = getLeftbf(bf);
                            // Only check if style is not "null"
                            if (!style.equals("null")) {
                                // Get style height and width, check if style can be used
                                int width = stylelist.dataconfig.getInt(style + ".width");
                                int height = stylelist.dataconfig.getInt(style + ".height");
                                for (int h = 0; h < height; h++) {
                                    for (int w = 0; w < width; w++) {
                                        BlockState b2 = b.getRelative(leftbf, w).getRelative(BlockFace.DOWN, h).getState();
                                        // If fail then end
                                        if (!(b2 instanceof Sign)) {
                                            playerErrorMsg(sender, String.format("The selected sign does not match height = %d and width = %d of the style %s.", height, width, style));
                                            return true;
                                        }
                                    }
                                }
                            }
                            Location loc = b.getLocation();
                            // Set arguments
                            String pidspath = sta + "." + plat + ".locations." + pidsno;
                            if (!style.equals("null")) {
                                stapidslist.dataconfig.set(sta + ".world", b.getWorld().getName());
                                stapidslist.dataconfig.set(pidspath + ".x", loc.getBlockX());
                                stapidslist.dataconfig.set(pidspath + ".y", loc.getBlockY());
                                stapidslist.dataconfig.set(pidspath + ".z", loc.getBlockZ());
                                stapidslist.dataconfig.set(pidspath + ".style", style);
                                sender.sendMessage(INPIDS_HEAD + ChatColor.GREEN + "Successfully set PIDS in " + pidspath + ".");
                            } else {
                                // If style is "null" then remove PIDS from stapidslist
                                stapidslist.dataconfig.set(pidspath, null);
                                sender.sendMessage(INPIDS_HEAD + ChatColor.GREEN + "Successfully removed PIDS in " + pidspath + ".");
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
            case 3,4:
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