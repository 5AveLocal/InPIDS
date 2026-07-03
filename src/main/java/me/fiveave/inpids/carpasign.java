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
        String line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).replaceAll("\\|", " ");
        String linecolor = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line_color"));
        String type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).replaceAll("\\|", " ");
        String typecolor = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type_color"));
        StringBuilder strb = new StringBuilder();
        String doordir = Objects.requireNonNull(pastylelist.dataconfig.getString(style + ".doordir." + stl.getDoorDir().get(thisstaindex)));
        // Station counter
        int i = 0;
        for (String s : stylelines) {
            int selindex = i - thisstaindex;
            String appendedstr = s;
            boolean append = true;
            // Header
            appendedstr = appendedstr.replaceAll("%line_color", linecolor)
                    .replaceAll("%line", line)
                    .replaceAll("%type_color", typecolor)
                    .replaceAll("%type", type);
            // Station display
            if (i >= stl.getSize() && (s.contains("%sta_") || s.contains("%trans_") || s.contains("%line_color"))) {
                append = false;
            } else if (i < stl.getSize()) {
                appendedstr = appendedstr.replaceAll("%sta_" + selindex, String.join(" ", stl.getStaname().get(i)))
                        .replaceAll("%trans_" + selindex, String.join(" ", stl.getTransfers().get(i)));
            }
            // Door direction
            appendedstr = appendedstr.replaceAll("%door_dir", doordir)
                    .replaceAll("\\\\&", "\\\\and") // To keep & type \&
                    .replaceAll("&", "§")
                    .replaceAll("\\\\and", "&");
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