package me.fiveave.inpids;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static me.fiveave.inpids.main.*;
import static me.fiveave.inpids.pidsupdate.updateSinglePidsDisplay;
import static me.fiveave.inpids.statimelist.getTimeToStation;

class platpidssys {
    Set<String> pidsset;
    String stacode;
    String plat;
    ArrayList<deprec> depreclist;
    boolean reqsort;

    platpidssys(String stacode, String plat) {
        depreclist = new ArrayList<>();
        pidsset = new HashSet<>();
        this.stacode = stacode;
        this.plat = plat;
        String staplat = stacode + "." + plat;
        String locpath = staplat + ".locations";
        ConfigurationSection cs = stapidslist.dataconfig.getConfigurationSection(locpath);
        if (cs != null) {
            pidsset = cs.getKeys(false);
        }
        // Start clock
        Bukkit.getScheduler().runTaskLater(plugin, this::clock, 1);
    }

    void clock() {
        // Update time
        ArrayList<deprec> deldrlist = new ArrayList<>();
        for (deprec dr : depreclist) {
            if (dr != null) {
                String trainname = dr.getName();
                int settime = getTimeToStation(trainname, stacode);
                // If train has passed station or train does not exist
                if (settime == Integer.MIN_VALUE || !trainlist.dataconfig.contains(trainname)) {
                    deldrlist.add(dr);
                } else {
                    dr.setTime(settime);
                }
            }
        }
        // Remove those who has to be removed
        deldrlist.forEach(this::removeDeprec);
        // Sort if needed
        if (reqsort) {
            sortPidsList();
        }
        // Update PIDS display
        for (String pids : pidsset) {
            updateSinglePidsDisplay(stacode, plat, depreclist, pids, false);
        }
        // Loop every tick unless depreclist is empty
        Bukkit.getScheduler().runTaskLater(plugin, this::clock, 1);
    }

    void addOrModifyDeprec(deprec dr) {
        boolean found = false;
        for (deprec dr0 : depreclist) {
            if (dr0.getName().equals(dr.getName())) {
                dr0.setTime(dr.getTime());
                found = true;
            }
        }
        // If not found
        if (!found) {
            depreclist.add(dr);
        }
        reqsort = true;
    }

    // TODO: Find out why departure records are not cleared on PIDS when train is deleted / goes to next station
    void removeDeprec(deprec dr) {
        if (depreclist.contains(dr)) {
            // Remove PIDS line
            for (String pids : pidsset) {
                updateSinglePidsDisplay(stacode, plat, depreclist, pids, true);
            }
            depreclist.remove(dr);
            reqsort = true;
        }
    }

    void sortPidsList() {
        depreclist.sort(Comparator.comparingDouble(deprec::getTime));
        reqsort = false;
    }
}
