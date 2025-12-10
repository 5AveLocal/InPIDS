package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.signactions.SignAction;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

public final class main extends JavaPlugin {
    static main plugin;
    static absyaml linetypelist, stylelist, trainlist, stapidslist;
    final inpidsupdate var0 = new inpidsupdate();

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        linetypelist = new absyaml(this, "linetypelist.yml");
        stylelist = new absyaml(this, "stylelist.yml");
        trainlist = new absyaml(this, "trainlist.yml");
        stapidslist = new absyaml(this, "stapidslist.yml");
        SignAction.register(var0);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        SignAction.unregister(var0);
    }

    static void errorLog(IOException e) {
        Bukkit.getLogger().log(Level.SEVERE, e.getMessage());
    }
}
