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

import java.util.List;
import java.util.Objects;

import static me.fiveave.inpids.deprec.updatePlatPidsList;
import static me.fiveave.inpids.main.*;

public class inpidsupdate extends SignAction {

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
            // Train info
            MinecartGroup mg = cartevent.getGroup();
            String trainname = mg.getProperties().getTrainName();
            // Update trainlist
            trainlist.dataconfig.set(trainname + ".linesys", linesys);
            trainlist.dataconfig.set(trainname + ".location", location);
            trainlist.dataconfig.set(trainname + ".time", time);
            trainlist.save();
            // Start PIDS Clock Loop (if not yet started)
            if (!pidsclock) {
                pidsClockLoop();
                pidsclock = true;
            }
        }
    }

    void pidsClockLoop() {
        for (String trainname : trainlist.dataconfig.getKeys(false)) {
            String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
            statimelist stl = new statimelist(linesys);
            for (int staindex = 0; staindex < stl.getStacode().length; staindex++) {
                String stacode = stl.getStacode()[staindex];
                int plat = stl.getPlat()[staindex];
                World world = Bukkit.getWorld(Objects.requireNonNull(stapidslist.dataconfig.getString(stacode + ".world")));
                // Update PIDS list
                updatePlatPidsList(stacode, plat, trainname);
                // Update PIDS display
                updatePlatPidsDisplay(stacode, plat, world);
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::pidsClockLoop, 1);
    }

    // Update all PIDS display on platform
    void updatePlatPidsDisplay(String stacode, int plat, World w) {
        String staplat = stacode + "." + plat;
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
            updateSinglePidsDisplay(stacode, plat, loc, pidsstyle);
        }
    }

    // Update single PIDS display on platform
    void updateSinglePidsDisplay(String stacode, int plat, Location loc, String pidsstyle) {
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
                        try {
                            int time = stapidslist.dataconfig.getInt(staplat + ".departures." + count + ".time");
                            String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
                            String[] line = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".line")).split("\\|");
                            String[] type = Objects.requireNonNull(linetypelist.dataconfig.getString(linesys + ".type")).split("\\|");
                            statimelist stl = new statimelist(linesys);
                            int terminusindex = stl.getStaname().length - 1;
                            String[] destination = stl.getStaname()[terminusindex];
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
                            boolean stop = stl.getStop()[stl.getStaIndex(stacode)];
                            boolean atterminus = stl.getStacode()[terminusindex].equals(stacode);
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
                                    .replaceAll("%tmin", String.valueOf(mtime > 0 ? (mtime + min) : time == 0 ? trainstopping : (thisflash ? (stop ? trainarr : trainpass) : "")))
                                    .replaceAll("\\\\&", "\\\\and") // To keep & type \&
                                    .replaceAll("&", "§")
                                    .replaceAll("\\\\and", "&");
                        } catch (Exception e) {
                            // If anything null then set to blank
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


}