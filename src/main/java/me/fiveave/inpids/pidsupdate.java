package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

import static me.fiveave.inpids.deprec.updatePlatPidsList;
import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.statimelist.getTimeToStation;

public class pidsupdate {

    static final HashMap<String, String> signtotext = new HashMap<>();

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

    static void pidsClockLoop() {
        Set<String> trainnameset = trainlist.dataconfig.getKeys(false);
        for (String trainname : trainnameset) {
            String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
            statimelist stl = stlmap.get(linesys);
            for (int staindex = 0; staindex < stl.getStacode().size(); staindex++) {
                try {
                    String stacode = stl.getStacode().get(staindex);
                    int plat = stl.getPlat().get(staindex);
                    // Update PIDS list
                    updatePlatPidsList(stacode, plat, trainname);
                    // Update PIDS display
                    updatePlatPidsDisplay(stacode, plat);
                } catch (Exception ignored) {
                }
            }
            // Subtract time
            String timepath = trainname + ".time";
            int timenow = trainlist.dataconfig.getInt(timepath);
            if (timenow > 0 && Math.toIntExact((System.currentTimeMillis() / 50) % 20) == 0) {
                trainlist.dataconfig.set(timepath, timenow - 1);
            }
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
                    if (pidsstyle == null) {
                        continue;
                    }
                    updateSinglePidsDisplay(stacode, plat, pidsstyle, locindex);
                } catch (Exception ignored) {
                }
            }
        }
    }

    // Update single PIDS display on platform
    static void updateSinglePidsDisplay(String stacode, int plat, String pidsstyle, String locindex) {
        String staplat = stacode + "." + plat;
        // Get data from stylelist.yml
        int height = stylelist.dataconfig.getInt(pidsstyle + ".height");
        List<Integer> lines = stylelist.dataconfig.getIntegerList(pidsstyle + ".lines");
        int loopinterval = stylelist.dataconfig.getInt(pidsstyle + ".loopinterval");
        int flashinterval = stylelist.dataconfig.getInt(pidsstyle + ".flashinterval");
        String pospath = staplat + ".locations." + locindex + ".pos";
        ConfigurationSection cs = Objects.requireNonNull(stapidslist.dataconfig.getConfigurationSection(pospath));
        Set<String> locset = cs.getKeys(false);
        ArrayList<Location> loclist = new ArrayList<>();
        for (String strloc : locset) {
            loclist.add(stapidslist.dataconfig.getLocation(pospath + "." + strloc));
        }
        long ticks = System.currentTimeMillis() / 50;
        int signsize = loclist.size();
        int linesize = lines.size();
        for (int count = 0; count < linesize * height; count++) {
            for (int i = 0; i < signsize; i++) {
                // Variables
                String onestyle = stylelist.dataconfig.getString(pidsstyle + ".style." + i);
                String trainname = stapidslist.dataconfig.getString(staplat + ".departures." + count + ".name");
                // Display variables
                int displine = lines.get(count % linesize);
                String dispstr = null;
                boolean trainnull = false;
                if (trainname != null) {
                    try {
                        int time = stapidslist.dataconfig.getInt(staplat + ".departures." + count + ".time");
                        String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
                        String location = trainlist.dataconfig.getString(trainname + ".location");
                        String stat = Objects.requireNonNull(trainlist.dataconfig.getString(trainname + ".stat"));
                        String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
                        String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
                        statimelist stl = stlmap.get(linesys);
                        int terminusindex = stl.getStaname().size() - 1;
                        String[] destination = stl.getStaname().get(terminusindex);
                        // Language selector (by current time)
                        int langsize = destination.length;
                        int thislang = Math.toIntExact(ticks % ((long) loopinterval * langsize) / loopinterval);
                        // Flash selector
                        boolean thisflash = Math.toIntExact(ticks % ((long) flashinterval * 2) / flashinterval) == 0;
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
                                .replaceAll("%tmin", String.valueOf(stat.equals("drive") || !stacode.equals(location) ? (mtime + min) : stat.equals("stop") ? trainstopping : (thisflash ? (stop ? trainarr : trainpass) : "")))
                                .replaceAll("\\\\&", "\\\\and") // To keep & type \&
                                .replaceAll("&", "§")
                                .replaceAll("\\\\and", "&");
                    } catch (Exception ignored) {
                        // If anything null then set to blank
                        TrainProperties tp = TrainProperties.get(trainname);
                        if (tp == null || !tp.getHolder().isValid()) {
                            trainnull = true;
                        }
                    }
                }
                // Set sign (format: stacode.plat.locations.pidsno.signno.lineno)
                String thispospath = pospath + "." + i + "." + displine;
                try {
                    // Update only if sign is different than last tick (requires non-null), or train is null
                    // If train has passed station then set to blank
                    boolean dispstrnull = dispstr == null;
                    boolean passedsta = getTimeToStation(trainname, stacode) == Integer.MIN_VALUE;
                    String setstr = dispstrnull || passedsta ? "" : dispstr;
                    if (trainnull || (!dispstrnull || passedsta) && (!signtotext.containsKey(thispospath) || !signtotext.get(thispospath).equals(setstr))) {
                        Sign sign2 = (Sign) loclist.get(i).getBlock().getState();
                        sign2.setLine(displine, setstr);
                        sign2.update();
                        signtotext.put(thispospath, setstr);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}