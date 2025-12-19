package me.fiveave.inpids;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static me.fiveave.inpids.deprec.updatePlatPidsList;
import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.statimelist.getTimeToStation;

public class pidsupdate {
    static BlockFace getLeftbf(BlockFace bf) {
        return switch (bf) {
            case EAST -> BlockFace.NORTH;
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            default -> bf;
        };
    }

    private static String[] getSplitStyleMsg(String pidsstyle, String path) {
        return Objects.requireNonNull(stylelist.dataconfig.getString(pidsstyle + ".messages." + path)).split("\\|");
    }

    static void updatePidsOnLine(String linesys) {
        statimelist stl = new statimelist(linesys);
        Set<String> trainnameset = trainlist.dataconfig.getKeys(false);
        HashSet<String> trainsonline = new HashSet<>();
        for (String trainname : trainnameset) {
            String trainlinesys = trainlist.dataconfig.getString(trainname + ".linesys");
            if (trainlinesys != null && trainlinesys.equals(linesys)) {
                trainsonline.add(trainname);
            }
        }
        for (String trainname : trainsonline) {
            for (int staindex = 0; staindex < stl.getStacode().size(); staindex++) {
                String stacode = stl.getStacode().get(staindex);
                int plat = stl.getPlat().get(staindex);
                String staplat = stacode + "." + plat;
                String deppath = staplat + ".departures";
                ConfigurationSection cs = stapidslist.dataconfig.getConfigurationSection(deppath);
                if (cs != null) {
                    Set<String> depset = cs.getKeys(false);
                    // Find if train exists
                    int foundtrainindex = findTrainIndex(depset, staplat, trainname);
                    int arrtime = getTimeToStation(trainname, stacode);
                    // Add or update train in PIDS info
                    String thisdeppath = deppath + "." + foundtrainindex;
                    boolean notfound = foundtrainindex == -1;
                    int max = 0;
                    if (notfound) {
                        for (String dep : depset) {
                            int intdep = Integer.parseInt(dep);
                            if (intdep > max) {
                                max = intdep;
                            }
                        }
                        thisdeppath = deppath + "." + max + 1;
                    }
                    if (arrtime == Integer.MIN_VALUE) {
                        // Delete record
                        stapidslist.dataconfig.set(thisdeppath, null);
                    } else {
                        // Add or set record
                        stapidslist.dataconfig.set(thisdeppath + ".name", trainname);
                        stapidslist.dataconfig.set(thisdeppath + ".time", arrtime);
                    }
                    stapidslist.save();
                }
            }
        }
    }

    static int findTrainIndex(Set<String> depset, String staplat, String trainname) {
        for (String dep : depset) {
            String deppath = staplat + ".departures." + dep;
            deprec dr = new deprec(deppath);
            String deptrainname = dr.getName();
            if (deptrainname != null && deptrainname.equals(trainname)) {
                return Integer.parseInt(dep);
            }
        }
        return -1;
    }

    static void pidsClockLoop() {
        HashSet<String> linesysset = new HashSet<>();
        // Get all lines
        Set<String> trainnameset = trainlist.dataconfig.getKeys(false);
        boolean isempty = trainnameset.isEmpty();
        for (String trainname : trainnameset) {
            String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
            linesysset.add(linesys);
        }
        // For each line
        for (String linesys : linesysset) {
            updatePidsOnLine(linesys);
            try {
                statimelist stl = new statimelist(linesys);
                for (int staindex = 0; staindex < stl.getStacode().size(); staindex++) {
                    String stacode = stl.getStacode().get(staindex);
                    int plat = stl.getPlat().get(staindex);
                    // Update PIDS list
                    updatePlatPidsList(stacode, plat);
                    // Update PIDS display
                    updatePlatPidsDisplay(stacode, plat);
                }
            } catch (Exception ignored) {
            }
        }
        // Subtract time
        for (String trainname : trainnameset) {
            String timepath = trainname + ".time";
            int timenow = trainlist.dataconfig.getInt(timepath);
            if (timenow > 0 && Math.toIntExact((System.currentTimeMillis() / 50) % 20) == 0) {
                trainlist.dataconfig.set(timepath, timenow - 1);
            }
        }
        // Save files
        if (!isempty) {
            trainlist.save();
            stapidslist.save();
        }
        Bukkit.getScheduler().runTaskLater(plugin, pidsupdate::pidsClockLoop, 1);
    }

    // Update all PIDS display on platform
    static void updatePlatPidsDisplay(String stacode, int plat) {
        String staplat = stacode + "." + plat;
        String locpath = staplat + ".locations";
        ConfigurationSection cs = stapidslist.dataconfig.getConfigurationSection(locpath);
        if (cs != null) {
            for (String locindex : cs.getKeys(false)) {
                try {
                    String indexpath = staplat + ".locations." + locindex;
                    String stylepath = indexpath + ".style";
                    // Get PIDS display style
                    String pidsstyle = stapidslist.dataconfig.getString(stylepath);
                    if (pidsstyle == null) continue;
                    Location loc = stapidslist.dataconfig.getLocation(indexpath + ".pos");
                    if (loc == null) continue;
                    updateSinglePidsDisplay(stacode, plat, loc, pidsstyle);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // Update single PIDS display on platform
    static void updateSinglePidsDisplay(String stacode, int plat, Location loc, String pidsstyle) {
        String staplat = stacode + "." + plat;
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
            int flashinterval = stylelist.dataconfig.getInt(pidsstyle + ".flashinterval");
            // Get surrounding signs for update
            HashSet<Sign> updatesignlist = new HashSet<>();
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
                        try {
                            int time = stapidslist.dataconfig.getInt(staplat + ".departures." + count + ".time");
                            String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
                            String location = trainlist.dataconfig.getString(trainname + ".location");
                            String stat = Objects.requireNonNull(trainlist.dataconfig.getString(trainname + ".stat"));
                            String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
                            String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
                            statimelist stl = new statimelist(linesys);
                            int terminusindex = stl.getStaname().size() - 1;
                            String[] destination = stl.getStaname().get(terminusindex);
                            // Language selector (by current time)
                            int langsize = destination.length;
                            int thislang = Math.toIntExact((System.currentTimeMillis() / 50) % ((long) loopinterval * langsize) / loopinterval);
                            // Flash selector
                            boolean thisflash = Math.toIntExact((System.currentTimeMillis() / 50) % ((long) flashinterval * 2) / flashinterval) == 0;
                            // Language split
                            assert onestyle != null;
                            String[] splitonelang = onestyle.split("\\|");
                            String onelangstyle = splitonelang[Math.min(splitonelang.length - 1, thislang)];
                            // Variable replacement
                            int mtime = (int) (time / 60.0);
                            boolean stop = stl.getStop().get(stl.getStaIndex(stacode));
                            boolean atterminus = stl.getStacode().get(terminusindex).equals(stacode);
                            String dest = destination[thislang];
                            String trainarr = getSplitStyleMsg(pidsstyle, "trainarr")[thislang];
                            String trainpass = getSplitStyleMsg(pidsstyle, "trainpass")[thislang];
                            String trainstopping = getSplitStyleMsg(pidsstyle, "trainstopping")[thislang];
                            String typepass = getSplitStyleMsg(pidsstyle, "typepass")[thislang];
                            String terminus = getSplitStyleMsg(pidsstyle, "terminus")[thislang];
                            String notinservice = getSplitStyleMsg(pidsstyle, "notinservice")[thislang];
                            String min = getSplitStyleMsg(pidsstyle, "min")[thislang];
//                             String[] delay = Objects.requireNonNull(stylelist.dataconfig.getString(pidsstyle + ".messages.delay")).split("\\|");
                            dispstr = onelangstyle.replaceAll("%type", atterminus ? notinservice : (stop ? type[thislang] : typepass))
                                    .replaceAll("%line", line[thislang])
                                    .replaceAll("%dest", String.valueOf(!atterminus ? dest : terminus))
                                    .replaceAll("%tmin", String.valueOf((stat.equals("drive") || !stacode.equals(location)) ? (mtime + min) : stat.equals("stop") ? trainstopping : (thisflash ? (stop ? trainarr : trainpass) : "")))
                                    .replaceAll("\\\\&", "\\\\and") // To keep & type \&
                                    .replaceAll("&", "§")
                                    .replaceAll("\\\\and", "&");
                        } catch (Exception e) {
                            // If anything null then set to blank
                            dispstr = "";
                        }
                        // Set sign if different
                        int line = lines.get(count % lines.size());
                        String olds2 = sign2.getLine(line);
                        if (!olds2.equals(dispstr)) {
                            sign2.setLine(line, dispstr);
                            updatesignlist.add(sign2);
                        }
                    }
                }
            }
            // Update signs
            for (Sign sign : updatesignlist) {
                sign.update();
            }
        }
    }
}
