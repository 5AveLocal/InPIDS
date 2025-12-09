package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.bergerkiller.bukkit.tc.signactions.SignAction;
import com.bergerkiller.bukkit.tc.signactions.SignActionType;
import com.bergerkiller.bukkit.tc.utils.SignBuildOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.statimelist.getTimeToStation;

public class inpidsupdate extends SignAction {

    private static BlockFace getLeftbf(BlockFace bf) {
        return switch (bf) {
            case EAST -> BlockFace.NORTH;
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            default -> bf;
        };
    }

    @Override
    public boolean match(SignActionEvent info) {
        return info.isType("inpidsupdate");
    }

    @Override
    public void execute(SignActionEvent cartevent) {
        if (cartevent.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON) && cartevent.hasRailedMember() && cartevent.isPowered()) {
            // Get sign info
            String linesys = cartevent.getLine(2); // linesys includes both line name and train type
            String[] l3 = cartevent.getLine(3).split(" ");
            String location = l3[0]; // Location: station on linesys
            int time = Integer.parseInt(l3[1]); // Time left in seconds
            // Update trainlist
            MinecartGroup mg = cartevent.getGroup();
            String trainname = mg.getProperties().getTrainName();
            trainlist.dataconfig.set(trainname + ".linesys", linesys);
            trainlist.dataconfig.set(trainname + ".location", location);
            trainlist.dataconfig.set(trainname + ".time", time);
            trainlist.save();
            // For stations on list at and after location, update PIDS on the linesys
            statimelist stl = new statimelist(linesys);
            String[] stacode = stl.getStacode();
            for (int i = stl.getStaIndex(location); i < stacode.length; i++) {
                // Station name + platform number
                String staplat = stacode[i] + "." + stl.getPlat()[i];
                World world = Bukkit.getWorld(Objects.requireNonNull(stapidslist.dataconfig.getString(stacode[i] + ".world")));
                // Update PIDS list
                updatePlatPidsList(staplat, trainname);
                // Update PIDS display
                updatePlatPidsDisplay(staplat, world);
            }
        }
    }

    // Modify and sort PIDS list data for a platform
    private void updatePlatPidsList(String staplat, String trainname) {
        // Get PIDS list, convert to record
        ArrayList<deprec> depreclist = new ArrayList<>();
        int foundtrainindex = -1;
        String deppath = staplat + ".departures";
        for (int dep : stapidslist.dataconfig.getIntegerList(deppath)) {
            deprec dr = new deprec(staplat + ".departures." + dep);
            depreclist.add(dr);
            if (dr.getName().equals(trainname)) {
                foundtrainindex = dep;
            }
        }
        // Modify or add this train
        if (foundtrainindex != -1) {
            depreclist.get(foundtrainindex).setTime(getTimeToStation(trainname));
        } else {
            depreclist.add(new deprec(staplat + ".departures." + depreclist.size()));
        }
        // Sort records by arrival times
        ArrayList<deprec> newdepreclist = new ArrayList<>();
        while (!depreclist.isEmpty()) {
            int minindex = getMinPidsRecIndex(depreclist);
            newdepreclist.add(depreclist.get(minindex));
            depreclist.remove(minindex);
        }
        // Update PIDS list
        for (int dep = 0; dep < newdepreclist.size(); dep++) {
            String pidspath = staplat + ".departures." + dep;
            stapidslist.dataconfig.set(pidspath + ".name", newdepreclist.get(dep).getName());
//                    stapidslist.dataconfig.set(pidspath + ".type", newdepreclist.get(dep).getType());
            stapidslist.dataconfig.set(pidspath + ".time", newdepreclist.get(dep).getTime());
        }
        stapidslist.save();
    }

    // Update all PIDS display on platform
    private void updatePlatPidsDisplay(String staplat, World w) {
        String locpath = staplat + ".locations.";
        for (int locindex : stapidslist.dataconfig.getIntegerList(locpath)) {
            String indexpath = staplat + ".locations." + locindex;
            int x = stapidslist.dataconfig.getInt(indexpath + ".x");
            int y = stapidslist.dataconfig.getInt(indexpath + ".y");
            int z = stapidslist.dataconfig.getInt(indexpath + ".z");
            Location loc = new Location(w, x, y, z);
            updateSinglePidsDisplay(staplat, loc);
        }
    }

    // Update single PIDS display on platform
    private void updateSinglePidsDisplay(String staplat, Location loc) {
        // Get PIDS display style
        String stylepath = staplat + ".style";
        String pidsstyle = stapidslist.dataconfig.getString(stylepath);
        Block b = loc.getBlock();
        BlockState bs = b.getState();
        if (bs instanceof Sign) {
            org.bukkit.block.data.type.Sign sign1 = (org.bukkit.block.data.type.Sign) b.getBlockData();
            BlockFace bf = sign1.getRotation();
            // Get data from stylelist.yml
            int width = stylelist.dataconfig.getInt(pidsstyle + ".width");
            int height = stylelist.dataconfig.getInt(pidsstyle + ".height");
            List<Integer> lines = stylelist.dataconfig.getIntegerList(pidsstyle + ".lines");
            int loopinterval = stylelist.dataconfig.getInt(pidsstyle + ".loopinterval");
            // Get surrounding signs
            BlockFace leftbf = getLeftbf(bf);
            for (int count = 0; count < lines.size() * height; count++) {
                int h = count / width;
                for (int w = 0; w < width; w++) {
                    BlockState b2 = b.getRelative(leftbf, w).getRelative(BlockFace.DOWN, h).getState();
                    if (b2 instanceof Sign sign2) {
                        // Variables
                        String onestyle = stylelist.dataconfig.getString(pidsstyle + ".style." + (h * width + w));
                        String trainname = stapidslist.dataconfig.getString(staplat + ".departures." + count + ".name");
                        int time = stapidslist.dataconfig.getInt(staplat + ".departures." + count + ".time");
                        String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
                        String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
                        String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
                        statimelist stl = new statimelist(linesys);
                        String[] destination = stl.getStaname()[stl.getStaname().length - 1];
                        // Language selector (by current time)
                        int langsize = destination.length;
                        int thislang = Math.toIntExact((System.currentTimeMillis() / 1000) % ((long) loopinterval * langsize) / langsize);
                        assert onestyle != null;
                        String onelangstyle = onestyle.split("\\|")[thislang];
                        // Variable replacement
                        onelangstyle = onelangstyle.replace("%type", type[thislang])
                                .replace("%line", line[thislang])
                                .replace("%dest", destination[thislang])
                                .replace("%tmin", String.valueOf(Math.round(time / 60.0)));
                        // Set sign
                        sign2.setLine(lines.get(count / lines.size()), onelangstyle);
                    }
                }
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

    int getMinPidsRecIndex(ArrayList<deprec> pidsreclist) {
        int index = -1;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < pidsreclist.size(); i++) {
            int val = pidsreclist.get(i).getTime();
            if (val < min) {
                min = val;
                index = i;
            }
        }
        return index;
    }
}