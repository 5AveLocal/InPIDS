package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.ChatColor;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.pidsupdate.pidsClockLoop;

public class updatesign extends SignAction {


    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("inpidsupdate");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            // Train info
            MinecartGroup mg = cartevent.getGroup();
            String trainname = mg.getProperties().getTrainName();
            // Get sign info
            String linesys = cartevent.getLine(2); // linesys includes both line name and train type
            statimelist stl = stlmap.get(linesys);
            String[] l3 = cartevent.getLine(3).split(" ");
            String location = l3[0]; // Location: station on linesys
            String oldlocation = trainlist.dataconfig.getString(trainname + ".location");
            String stat = null; // Train status: drive / arr / stop
            // If location is different then stat is "drive" by default
            if (oldlocation != null && !oldlocation.equals(location)) {
                stat = "drive";
            }
            int time; // Time left in seconds
            if (l3.length > 1) {
                try {
                    time = Integer.parseInt(l3[1]);
                } catch (Exception e) {
                    stat = l3[1];
                    time = 0;
                }
            } else {
                time = stl.getTime().get(stl.getStaIndex(location));
            }
            // Update trainlist
            trainlist.dataconfig.set(trainname + ".linesys", linesys);
            trainlist.dataconfig.set(trainname + ".location", location);
            trainlist.dataconfig.set(trainname + ".time", time);
            // If stat is set
            if (stat != null) {
                trainlist.dataconfig.set(trainname + ".stat", stat);
            }
            trainlist.save();
            // Start PIDS Clock Loop (if not yet started)
            if (!pidsclock) {
                pidsClockLoop();
                pidsclock = true;
            }
        }
    }


    @Override
    public boolean build(SignChangeActionEvent e) {
        try {
            SignBuildOptions opt = SignBuildOptions.create().setName(ChatColor.GOLD + "PIDS Information Updater");
            opt.setDescription("Update PIDS information, will trigger changes according to database");
            return opt.handle(e.getPlayer());
        } catch (Exception exception) {
            e.getPlayer().sendMessage(ChatColor.RED + "Invalid arguments!");
            e.setCancelled(true);
        }
        return true;
    }


}