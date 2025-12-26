package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.statimelist.getTimeToStation;

public class pidsupdate {

    static final HashMap<String, String> signtotext = new HashMap<>();
    static final HashMap<Location, Sign> loctosign = new HashMap<>();

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

    // Update single PIDS display on platform
    static void updateSinglePidsDisplay(String stacode, String plat, ArrayList<deprec> depreclist, String pidsindex) {
        String staplat = stacode + "." + plat;
        String indexpath = stacode + "." + plat + ".locations." + pidsindex;
        String stylepath = indexpath + ".style";
        String pidsstyle = stapidslist.dataconfig.getString(stylepath);
        // Get data from stylelist.yml
        int height = stylelist.dataconfig.getInt(pidsstyle + ".height");
        List<Integer> lines = stylelist.dataconfig.getIntegerList(pidsstyle + ".lines");
        int loopinterval = stylelist.dataconfig.getInt(pidsstyle + ".loopinterval");
        int flashinterval = stylelist.dataconfig.getInt(pidsstyle + ".flashinterval");
        String pospath = staplat + ".locations." + pidsindex + ".pos";
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
                // Display variables
                int displine = lines.get(count % linesize);
                String dispstr = null;
                boolean trainnull = false;
                String trainname = null;
                // Skip and set to blank if cannot find
                if (count < depreclist.size()) {
                    deprec thisdeprec = depreclist.get(count);
                    trainname = thisdeprec.getName();
                    int time = thisdeprec.getTime();
                    if (trainname != null) {
                        try {
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
                            boolean thisflash = ticks % ((long) flashinterval * 2) / flashinterval == 0;
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
                            //String[] delay = Objects.requireNonNull(stylelist.dataconfig.getString(pidsstyle + ".messages.delay")).split("\\|");
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
                }
                // Set sign (format: stacode.plat.locations.pidsno.signno.lineno)
                String thispospath = pospath + "." + i + "." + displine;
                try {
                    // Update only if sign is different than last tick (requires non-null), or train is null
                    // If train has passed station then set to blank
                    boolean dispstrnull = dispstr == null;
                    boolean passedsta = getTimeToStation(trainname, stacode) == Integer.MIN_VALUE;
                    String setstr = trainnull || dispstrnull || passedsta ? "" : dispstr;
                    signtotext.putIfAbsent(thispospath, "");
                    if (!signtotext.get(thispospath).equals(setstr)) {
                        Location setloc = loclist.get(i);
                        // Need to let it get state or else sign will be either stuck, or not reset when train is destroyed
                        Sign sign2 = (Sign) setloc.getBlock().getState();
                        sign2.setLine(displine, setstr);
                        // Update function is unstable, ignore its effects (since 1.20)
                        sign2.update();
                        signtotext.put(thispospath, setstr);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}