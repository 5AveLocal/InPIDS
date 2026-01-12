package me.fiveave.inpids;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.statimelist.getTimeToStation;

/// PIDS updating class
class pidsupdate {

    /// HashMap of text on sign
    static final HashMap<String, String> signtotext = new HashMap<>();

    /// Gets BlockFace relative to left of sign
    ///
    /// @param bf BlockFace of the sign
    /// @return BlockFace relative to left of sign
    static BlockFace getLeftbf(BlockFace bf) {
        // Get BlockFace relative to left of sign
        return switch (bf) {
            case EAST -> BlockFace.NORTH;
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            default -> bf;
        };
    }

    /// Splits message into different languages with specified style
    ///
    /// @param style stylerec of the PIDS display
    /// @param path  Path of item (message) to be fetched
    /// @return String array of messages in different languages
    private static String[] getSplitStyleMsg(stylerec style, String path) {
        return Objects.requireNonNull(style.getMessages().get(path)).split("\\|");
    }

    /// Updates a single PIDS display on platform
    ///
    /// @param stacode    Station code
    /// @param plat       Platform number
    /// @param depreclist Departure record list
    /// @param pidsindex  PIDS display number
    // Update single PIDS display on platform
    static void updateSinglePidsDisplay(String stacode, String plat, ArrayList<deprec> depreclist, String pidsindex) {
        String staplat = stacode + "." + plat;
        String indexpath = stacode + "." + plat + ".locations." + pidsindex;
        String stylepath = indexpath + ".style";
        String pidsstyle = stapidslist.dataconfig.getString(stylepath);
        stylerec sr = stylemap.get(pidsstyle);
        // Get data from stylelist.yml
        int height = sr.getHeight();
        List<Integer> lines = sr.getLines();
        int loopinterval = sr.getLoopinterval();
        int flashinterval = sr.getFlashinterval();
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
                String onestyle = sr.getStyles().get(i);
                // Display variables
                int displine = lines.get(count % linesize);
                String dispstr = null;
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
                            int terminusindex = stl.getSize() - 1;
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
                            String trainarr = getSplitStyleMsg(sr, "trainarr")[thislang];
                            String trainpass = getSplitStyleMsg(sr, "trainpass")[thislang];
                            String trainstopping = getSplitStyleMsg(sr, "trainstopping")[thislang];
                            String typepass = getSplitStyleMsg(sr, "typepass")[thislang];
                            String terminus = getSplitStyleMsg(sr, "terminus")[thislang];
                            String notinservice = getSplitStyleMsg(sr, "notinservice")[thislang];
                            String min = getSplitStyleMsg(sr, "min")[thislang];
                            //String delay = getSplitStyleMsg(sr, "delay")[thislang];
                            // Display string replacements
                            dispstr = onelangstyle.replaceFirst("%type", atterminus ? notinservice : (stop ? type[thislang] : typepass))
                                    .replaceFirst("%line", line[thislang])
                                    .replaceFirst("%dest", String.valueOf(!atterminus ? dest : terminus))
                                    .replaceFirst("%tmin", String.valueOf(stat.equals("drive") || !stacode.equals(location) ? (mtime + min) : stat.equals("stop") ? trainstopping : (thisflash ? (stop ? trainarr : trainpass) : "")))
                                    .replaceAll("\\\\&", "\\\\and") // To keep & type \&
                                    .replaceAll("&", "§")
                                    .replaceAll("\\\\and", "&");
                        } catch (Exception ignored) {
                            // If anything null then display will be set to blank
                        }
                    }
                }
                // Set sign (format: stacode.plat.locations.pidsno.signno.lineno)
                String thispospath = pospath + "." + i + "." + displine;
                try {
                    // Update only if sign is different than last tick (requires non-null), or train is null
                    // If dispstr is null or train has passed station then set to blank
                    String setstr = dispstr == null || getTimeToStation(trainname, stacode) == Integer.MIN_VALUE ? "" : dispstr;
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