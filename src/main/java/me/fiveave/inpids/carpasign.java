package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.fiveave.inpids.main.*;

/// inpidscarpa sign class
class carpasign extends SignAction {

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("inpidscarpa");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            // Train info
            MinecartGroup mg = cartevent.getGroup();
            // Get sign info
            String linesys = cartevent.getLine(2); // linesys includes both line name and train type
            String[] l3 = cartevent.getLine(3).split(" ");
            String location = l3[0]; // Location: station on linesys
            String style = l3[1]; // PA text style (separate file for every status)
            // Getters
            statimelist stl = stlmap.get(linesys);
            if (stl == null) {
                errorLog(new Exception(linesys + ".csv does not exist!"));
                return;
            }
            // Run for each cart, get passengers to play PA
            mg.forEach(m -> inCarPaSystem(m, stl, linesys, location, style));
        }
    }

    // TODO: Finish the in-car PA system
    private void inCarPaSystem(MinecartMember<?> m, statimelist stl, String linesys, String location, String style) {
        int thisstaindex = stl.getStaIndex(location);
        List<String> stylelines = pastylelist.dataconfig.getStringList(style + ".text");
        String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
        String linecolor = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line_color"));
        String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
        String typecolor = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type_color"));
        StringBuilder strb = new StringBuilder();
        String doordir = Objects.requireNonNull(pastylelist.dataconfig.getString(style + ".doordir." + stl.getDoorDir().get(thisstaindex)));
        ArrayList<String[]> staname = stl.getStaname();
        ArrayList<String> transfers = stl.getTransfers();
        int stlsize = stl.getSize();
        int terminusindex = stlsize - 1;
        String[] dest = staname.get(terminusindex);
        // Station counter
        int i = 0;
        boolean stopouterloop = false;
        for (int selindex = i - thisstaindex; selindex  < stlsize - thisstaindex; selindex ++) {
            for (String s : stylelines) {
                if (s.contains("%sta_" + selindex)) {
                    i = selindex + thisstaindex;
                    stopouterloop = true;
                    break;
                }
            }
            if (stopouterloop) break;
        }
        for (String s : stylelines) {
            int selindex = i - thisstaindex;
            String appendedstr = s;
            boolean append = true;

            // Lines and train types
            // Replacement of specific languages
            for (int langcount = 0; langcount < dest.length; langcount++) {
                appendedstr = appendedstr.replace("%dest_" + langcount, dest[langcount]);
            }
            for (int langcount = 0; langcount < line.length; langcount++) {
                appendedstr = appendedstr.replace("%line_" + langcount, line[langcount]);
            }
            for (int langcount = 0; langcount < type.length; langcount++) {
                appendedstr = appendedstr.replace("%type_" + langcount, type[langcount]);
            }
            // General replacements
            appendedstr = appendedstr
                    .replace("%dest", String.join(" ", dest))
                    .replace("%line_color", linecolor)
                    .replace("%type_color", typecolor)
                    .replace("%line", String.join(" ", line))
                    .replace("%type", String.join(" ", type));

            // Station display
            if (i >= stlsize && (s.contains("%sta_") || s.contains("%trans_") || s.contains("%line_color"))) {
                append = false;
            } else if (i < stlsize) {
                // Replacement of specific languages (format: %<param>_<index>_<lang>)
                for (int langcount = 0; langcount < staname.get(i).length; langcount++) {
                    appendedstr = appendedstr.replace("%sta_" + selindex + "_" + langcount, staname.get(i)[langcount]);
                }
                // General replacements
                appendedstr = appendedstr
                        .replace("%sta_" + selindex, String.join(" ", staname.get(i)))
                        .replace("%trans_" + selindex, String.join(" ", transfers.get(i)));
            }

            // Door direction
            appendedstr = appendedstr.replace("%door_dir", doordir);
            // Color replacement
            appendedstr = colorparser.parseColors(appendedstr);
            // Appending and station counting
            if (append) {
                strb.append(appendedstr);
                strb.append("\n");
                if (s.contains("%trans_")) {
                    i++;
                }
            }
        }
        // Play announcement to passengers
        for (Entity e : m.getEntity().getPassengers()) {
            if (e instanceof Player p) {
                p.sendMessage(strb.toString());
            }
        }
    }

    @Override
    public boolean build(SignChangeActionEvent e) {
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "In-car Passenger Announcer");
            opt.setDescription("Play announcements based off statimelist");
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid arguments!");
            e.setCancelled(true);
        }
        return true;
    }
}