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

/// Main class
public final class main extends JavaPlugin {
    /// InPIDS plugin message header
    static final String INPIDS_HEAD = ChatColor.AQUA + "[" + ChatColor.YELLOW + "InPIDS" + ChatColor.AQUA + "] ";
    /// HashMap of station time lists
    static final HashMap<String, statimelist> stlmap = new HashMap<>();
    /// HashMap of platform PIDS records
    static final HashMap<String, platpidssys> pidsrecmap = new HashMap<>();
    /// HashMap of style records
    static final HashMap<String, stylerec> stylemap = new HashMap<>();
    /// This plugin
    static main plugin;
    /// Boolean on whether train list clock is running
    static boolean tlClock;
    static absyaml linetypelist, stylelist, trainlist, stapidslist, pastylelist;
    static boolean tlsave, splsave;
    /// inpidsupdate sign object
    static final updatesign var0 = new updatesign();
    static final carpasign var1 = new carpasign();

    /// Error log method
    ///
    /// @param e Exception
    static void errorLog(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, ChatColor.stripColor(INPIDS_HEAD) + "An error occurred!", e);
    }

    /// Returns a boolean on whether current tick is 0 in a second
    ///
    /// @return Boolean on whether current tick is 0 in a second
    static boolean isAtZeroTick() {
        return Math.toIntExact((System.currentTimeMillis() / 50) % 20) == 0;
    }

    /// Plugin enable method
    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        enableLogic();
    }

    static void enableLogic() {
        tlClock = false;
        tlsave = false;
        splsave = false;
        // Load all .yml files
        linetypelist = new absyaml(plugin, "linetypelist.yml");
        stylelist = new absyaml(plugin, "stylelist.yml");
        trainlist = new absyaml(plugin, "trainlist.yml");
        stapidslist = new absyaml(plugin, "stapidslist.yml");
        pastylelist = new absyaml(plugin, "pastylelist.yml");
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
        Objects.requireNonNull(plugin.getCommand("inpids")).setExecutor(new cmds());
        Objects.requireNonNull(plugin.getCommand("inpids")).setTabCompleter(new cmds());
        SignAction.register(var0);
        SignAction.register(var1);
    }

    /// Plugin disable method
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        disableLogic();
    }

    static void disableLogic() {
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
        SignAction.unregister(var1);
    }
}