package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

import static me.fiveave.inpids.pidsupdate.updateSinglePidsDisplay;

/**
 * Main class
 */
public final class main extends JavaPlugin {
    /**
     * InPIDS plugin message header
     */
    static final String INPIDS_HEAD = ChatColor.AQUA + "[" + ChatColor.YELLOW + "InPIDS" + ChatColor.AQUA + "] ";
    /**
     * HashMap of station time lists
     */
    static final HashMap<String, statimelist> stlmap = new HashMap<>();
    /**
     * HashMap of platform PIDS records
     */
    static final HashMap<String, platpidssys> pidsrecmap = new HashMap<>();
    /**
     * HashMap of style records
     */
    static final HashMap<String, stylerec> stylemap = new HashMap<>();
    /**
     * This plugin
     */
    static main plugin;
    /**
     * Boolean on whether train list clock is running
     */
    static boolean tlClock;
    /**
     * linetypelist.yml
     */
    static absyaml linetypelist, /**
     * stylelist.yml
     */
    stylelist, /**
     * trainlist.yml
     */
    trainlist, /**
     * stapidslist.yml
     */
    stapidslist;
    /**
     * Boolean on whether trainlist.yml should be saved
     */
    static boolean tlsave, /**
     * Boolean on whether stapidslist.yml should be saved
     */
    splsave;
    /**
     * inpidsupdate sign object
     */
    final updatesign var0 = new updatesign();

    /**
     * Error log method
     *
     * @param e Exception
     */
    static void errorLog(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
    }

    /**
     * Returns a boolean on whether current tick is 0 in a second
     *
     * @return Boolean on whether current tick is 0 in a second
     */
    static boolean isAtZeroTick() {
        return Math.toIntExact((System.currentTimeMillis() / 50) % 20) == 0;
    }

    /**
     * Plugin enable method
     */
    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        tlClock = false;
        tlsave = false;
        splsave = false;
        // Load all .yml files
        linetypelist = new absyaml(this, "linetypelist.yml");
        stylelist = new absyaml(this, "stylelist.yml");
        trainlist = new absyaml(this, "trainlist.yml");
        stapidslist = new absyaml(this, "stapidslist.yml");
        // Default statimelist "iwakinoup"
        String iwakinoup = "statimelist/iwakinoup.csv";
        if (!new File(plugin.getDataFolder() + "/" + iwakinoup).exists()) {
            plugin.saveResource(iwakinoup, false);
        }
        // Save all statimelist into HashMap to prevent creating new objects repeatedly
        File stlfolder = new File(plugin.getDataFolder() + "/statimelist");
        File[] stlfiles = stlfolder.listFiles();
        if (stlfiles != null) {
            for (File stlf : stlfiles) {
                String stlfname = stlf.getName();
                if (stlfname.contains(".csv")) {
                    String purestlfname = stlfname.substring(0, stlfname.lastIndexOf("."));
                    stlmap.put(purestlfname, new statimelist(purestlfname));
                }
            }
        }
        if (stlmap.isEmpty()) {
            errorLog(new Exception("Could not find .csv files in statimelist folder!"));
        }
        // Save all stylerec into HashMap to prevent repeated data fetching from files
        ConfigurationSection stylecs = Objects.requireNonNull(stylelist.dataconfig);
        Set<String> stylenameset = stylecs.getKeys(false);
        for (String stylename : stylenameset) {
            stylemap.put(stylename, new stylerec(stylename));
        }
        // Register commands and signs
        Objects.requireNonNull(this.getCommand("inpids")).setExecutor(new cmds());
        Objects.requireNonNull(this.getCommand("inpids")).setTabCompleter(new cmds());
        SignAction.register(var0);
    }

    /**
     * Plugin disable method
     */
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // Clear all PIDS displays
        for (String pidsrecstr : pidsrecmap.keySet()) {
            platpidssys pps = pidsrecmap.get(pidsrecstr);
            pps.depreclist.clear();
            for (String pids : pps.pidsset) {
                updateSinglePidsDisplay(pps.stacode, pps.plat, pps.depreclist, pids);
            }
        }
        // Save files
        trainlist.save();
        stapidslist.save();
        SignAction.unregister(var0);
    }
}