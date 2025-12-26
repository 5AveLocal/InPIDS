package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Set;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.statimelist.getTimeToStation;

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
            if (stl == null) {
                errorLog(new Exception(linesys + ".csv does not exist!"));
                return;
            }
            String[] l3 = cartevent.getLine(3).split(" ");
            String location = l3[0]; // Location: station on linesys
            String oldlocation = trainlist.dataconfig.getString(trainname + ".location");
            String oldlinesys = trainlist.dataconfig.getString(trainname + ".linesys");
            String stat = null; // Train status: drive / arr / stop
            boolean samelinesys = oldlinesys == null || linesys.equals(oldlinesys);
            // If location or linesys is different then stat is "drive" by default
            if (oldlocation != null && !oldlocation.equals(location) || !samelinesys) {
                stat = "drive";
            }
            // For old line (if different)
            if (!samelinesys) {
                statimelist oldstl = stlmap.get(oldlinesys);
                for (int i = 0; i < oldstl.getSize(); i++) {
                    String stacode = oldstl.getStacode().get(i);
                    String plat = oldstl.getPlat().get(i);
                    String staplat = stacode + "." + plat;
                    // Prepare info for platpidssys
                    int statime = getTimeToStation(trainname, stacode);
                    deprec dr = new deprec(trainname, statime);
                    // Get platpidssys
                    platpidssys pps = !pidsrecmap.containsKey(staplat) ? new platpidssys(stacode, plat) : pidsrecmap.get(staplat);
                    // Delete all
                    pps.removeDeprec(dr);
                    pidsrecmap.put(staplat, pps);
                }
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
            // For line
            for (int i = 0; i < stl.getSize(); i++) {
                String stacode = stl.getStacode().get(i);
                String plat = stl.getPlat().get(i);
                String staplat = stacode + "." + plat;
                // Prepare info for platpidssys
                int statime = getTimeToStation(trainname, stacode);
                deprec dr = new deprec(trainname, statime);
                // Create new or get platpidssys
                platpidssys pps = !pidsrecmap.containsKey(staplat) ? new platpidssys(stacode, plat) : pidsrecmap.get(staplat);
                // Delete if passed station or train is null, add or modify if exists and will arrive
                if (statime == Integer.MIN_VALUE) {
                    pps.removeDeprec(dr);
                } else {
                    // Add or modify
                    pps.addOrModifyDeprec(dr);
                }
                pidsrecmap.put(staplat, pps);
            }
            // Start trainlist clock loop (if not yet started)
            if (!tlClock) {
                trainlistClockLoop();
                tlClock = true;
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

    void trainlistClockLoop() {
        Set<String> trainnameset = trainlist.dataconfig.getKeys(false);
        for (String trainname : trainnameset) {
            // Subtract time
            String timepath = trainname + ".time";
            int timenow = trainlist.dataconfig.getInt(timepath);
            if (timenow > 0 && isAtZeroTick()) {
                trainlist.dataconfig.set(timepath, timenow - 1);
            }
            // If train does not exist then delete record
            TrainProperties tp = TrainProperties.get(trainname);
            if ((tp == null || !tp.getHolder().isValid()) && trainname != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    trainlist.dataconfig.set(trainname, null);
                    tlsave = true;
                }, 1);
            }
        }
        // Save to trainlist at once at end for all
        if (tlsave) {
            trainlist.save();
            tlsave = false;
        }
        // Save to stapidslist at once at end for all
        if (splsave) {
            stapidslist.save();
            splsave = false;
        }
        // Loop every tick unless pidsrecmap is empty (i.e. no PIDS running)
        if (!pidsrecmap.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, this::trainlistClockLoop, 1);
        } else {
            tlClock = false;
        }
    }
}