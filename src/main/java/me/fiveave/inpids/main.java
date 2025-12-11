package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public final class main extends JavaPlugin {
    static final String INPIDS_HEAD = ChatColor.AQUA + "[" + ChatColor.GREEN + "Untenshi" + ChatColor.AQUA + "] ";
    static main plugin;
    static boolean pidsclock;
    static absyaml linetypelist, stylelist, trainlist, stapidslist;
    final inpidsupdate var0 = new inpidsupdate();

    static void errorLog(Exception e) {
        Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        pidsclock = false;
        linetypelist = new absyaml(this, "linetypelist.yml");
        stylelist = new absyaml(this, "stylelist.yml");
        trainlist = new absyaml(this, "trainlist.yml");
        stapidslist = new absyaml(this, "stapidslist.yml");
        String iwakinoup = "statimelist/iwakinoup.csv";
        if (!new File(plugin.getDataFolder() + "/" + iwakinoup).exists()) {
            plugin.saveResource("statimelist/iwakinoup.csv", false);
        }
        Objects.requireNonNull(this.getCommand("inpids")).setExecutor(new cmds());
        Objects.requireNonNull(this.getCommand("inpids")).setTabCompleter(new cmds());
        SignAction.register(var0);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        pidsclock = false;
        SignAction.unregister(var0);
    }
}
