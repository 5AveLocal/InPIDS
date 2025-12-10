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
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;

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
            statimelist stl = new statimelist(linesys);
            String[] l3 = cartevent.getLine(3).split(" ");
            String location = l3[0]; // Location: station on linesys
            int time = l3.length > 1 ? Integer.parseInt(l3[1]) : stl.getTime()[stl.getStaIndex(location)]; // Time left in seconds
            // Update trainlist
            MinecartGroup mg = cartevent.getGroup();
            String trainname = mg.getProperties().getTrainName();
            trainlist.dataconfig.set(trainname + ".linesys", linesys);
            trainlist.dataconfig.set(trainname + ".location", location);
            trainlist.dataconfig.set(trainname + ".time", time);
            trainlist.save();
            // For stations on list at and after location, update PIDS on the linesys
            String[] stacode = stl.getStacode();
            for (int i = stl.getStaIndex(location); i < stacode.length; i++) {
                World world = Bukkit.getWorld(Objects.requireNonNull(stapidslist.dataconfig.getString(stacode[i] + ".world")));
                // Station name + platform number
                String staplat = stacode[i] + "." + stl.getPlat()[i];
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
        ConfigurationSection cs = stapidslist.dataconfig.getConfigurationSection(deppath);
        if (cs != null) {
            for (String dep : cs.getKeys(false)) {
                deprec dr = new deprec(staplat + ".departures." + dep);
                depreclist.add(dr);
                if (dr.getName().equals(trainname)) {
                    foundtrainindex = Integer.parseInt(dep);
                }
            }
        }
        // Modify or add this train
        if (foundtrainindex != -1) {
            depreclist.get(foundtrainindex).setTime(getTimeToStation(trainname));
        } else {
            deprec dr = new deprec(staplat + ".departures." + depreclist.size());
            dr.setName(trainname);
            dr.setTime(getTimeToStation(trainname));
            depreclist.add(dr);
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
        String locpath = staplat + ".locations";
        for (String locindex : Objects.requireNonNull(stapidslist.dataconfig.getConfigurationSection(locpath)).getKeys(false)) {
            String indexpath = staplat + ".locations." + locindex;
            String stylepath = indexpath + ".style";
            // Get PIDS display style
            String pidsstyle = stapidslist.dataconfig.getString(stylepath);
            int x = stapidslist.dataconfig.getInt(indexpath + ".x");
            int y = stapidslist.dataconfig.getInt(indexpath + ".y");
            int z = stapidslist.dataconfig.getInt(indexpath + ".z");
            Location loc = new Location(w, x, y, z);
            updateSinglePidsDisplay(staplat, loc, pidsstyle);
        }
    }

    // Update single PIDS display on platform
    private void updateSinglePidsDisplay(String staplat, Location loc, String pidsstyle) {
        Block b = loc.getBlock();
        BlockState bs = b.getState();
        if (bs instanceof Sign) {
            WallSign sign1 = (WallSign) b.getBlockData();
            BlockFace bf = sign1.getFacing();
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
                        // Display variables
                        String dispstr;
                        // If anything not found then set to blank
                        try {
                            int time = stapidslist.dataconfig.getInt(staplat + ".departures." + count + ".time");
                            String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
                            String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
                            String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
                            statimelist stl = new statimelist(linesys);
                            String[] destination = stl.getStaname()[stl.getStaname().length - 1];
                            // Language selector (by current time)
                            int langsize = destination.length;
                            int thislang = Math.toIntExact((System.currentTimeMillis() / 1000) % ((long) loopinterval * langsize) / loopinterval);
                            assert onestyle != null;
                            String[] splitonelang = onestyle.split("\\|");
                            String onelangstyle = splitonelang[Math.min(splitonelang.length - 1, thislang)];
                            // Variable replacement
                            dispstr = onelangstyle.replaceAll("%type", type[thislang])
                                    .replaceAll("%line", line[thislang])
                                    .replaceAll("%dest", destination[thislang])
                                    .replaceAll("%tmin", String.valueOf(Math.round(time / 60.0)))
                                    .replaceAll("\\\\&", "\\\\and") // To keep & type \&
                                    .replaceAll("&", "§")
                                    .replaceAll("\\\\and", "&");
                        } catch (Exception e) {
                            dispstr = "";
                        }
                        // Set sign
                        sign2.setLine(lines.get(count % lines.size()), dispstr);
                        sign2.update();
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