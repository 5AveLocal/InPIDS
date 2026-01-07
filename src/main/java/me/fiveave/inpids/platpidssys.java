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

/// Platform PIDS system class
class platpidssys {
    /// Set of PIDS displays
    Set<String> pidsset;
    /// Station code
    String stacode;
    /// Platform number
    String plat;
    /// List of departure record lists
    ArrayList<deprec> depreclist;

    /// @param stacode Station code
    /// @param plat    Platform number
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

    /// Platform PIDS loop clock (every tick)
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
        // Update PIDS display
        for (String pids : pidsset) {
            updateSinglePidsDisplay(stacode, plat, depreclist, pids);
        }
        // TODO: test if this works: Loop every tick unless depreclist is empty
        Bukkit.getScheduler().runTaskLater(plugin, this::clock, 1);
    }

    /// Adds of modifies departures record
    ///
    /// @param dr deprec object
    void addOrModifyDeprec(deprec dr) {
        boolean found = false;
        for (deprec dr0 : depreclist) {
            if (dr0.getName().equals(dr.getName())) {
                dr0.setTime(dr.getTime());
                found = true;
                break;
            }
        }
        // If not found
        if (!found) {
            depreclist.add(dr);
        }
        sortPidsList();
    }

    /// Removes departure record
    ///
    /// @param dr deprec object
    void removeDeprec(deprec dr) {
        for (deprec dr0 : depreclist) {
            /* deprec object may be newly created and thus has different signatures,
            even though contents of both are the same,
            so just compare train name and arrival time is sufficient */
            if (dr0.getName().equals(dr.getName()) && dr0.getTime() == dr.getTime()) {
                depreclist.remove(dr0);
                sortPidsList();
                break;
            }
        }
    }

    /// Sorts the PIDS list based on arrival time
    void sortPidsList() {
        depreclist.sort(Comparator.comparingDouble(deprec::getTime));
    }
}
