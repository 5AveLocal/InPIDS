package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

import static me.fiveave.inpids.main.stapidslist;
import static me.fiveave.inpids.main.trainlist;

public class deprec {
    // Departure records in a PIDS display
    private final String name; // Name of data (e.g. train name, info name)
    //    private String type; // Type of data (e.g. train, info)
    private final int time; // -1 for N/A

    deprec(String depno) {
        name = stapidslist.dataconfig.getString(depno + ".name");
//        type = stapidslist.dataconfig.getString(depno + ".type");
        time = stapidslist.dataconfig.getInt(depno + ".time");
    }

    // Modify and sort PIDS list data for a platform
    static void updatePlatPidsList(String stacode, int plat) {
        String staplat = stacode + "." + plat;
        // Add trains into list if valid, and find if list contains this train
        ArrayList<deprec> depreclist = getDeprecList(staplat);
        // Sort records by arrival times
        ArrayList<deprec> newdepreclist = getNewDeprecList(depreclist);
        // Update PIDS list
        updatePidsList(newdepreclist, staplat);
    }

    static ArrayList<deprec> getNewDeprecList(ArrayList<deprec> depreclist) {
        ArrayList<deprec> newdepreclist = new ArrayList<>();
        int size = depreclist.size();
        while (size > 0) {
            int minindex = getMinPidsRecIndex(depreclist);
            newdepreclist.add(depreclist.get(minindex));
            size--;
        }
        return newdepreclist;
    }

    static int getMinPidsRecIndex(ArrayList<deprec> pidsreclist) {
        int index = -1;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < pidsreclist.size(); i++) {
            int val = pidsreclist.get(i).getTime();
            if (val < min) {
                min = val;
                index = i;
            }
        }
        return index;
    }

    static void updatePidsList(ArrayList<deprec> newdepreclist, String staplat) {
        for (int dep = 0; dep < newdepreclist.size(); dep++) {
            String deppath = staplat + ".departures." + dep;
            deprec dr = newdepreclist.get(dep);
            stapidslist.dataconfig.set(deppath + ".name", dr.getName());
//                    stapidslist.dataconfig.set(deppath + ".type", dr.getType());
            stapidslist.dataconfig.set(deppath + ".time", dr.getTime());
        }
    }

    static ArrayList<deprec> getDeprecList(String staplat) {
        ArrayList<deprec> depreclist = new ArrayList<>();
        String deppath = staplat + ".departures";
        ConfigurationSection cs = stapidslist.dataconfig.getConfigurationSection(deppath);
        if (cs != null) {
            for (String dep : cs.getKeys(false)) {
                String pidspath = deppath + "." + dep;
                deprec dr = new deprec(pidspath);
                String trainname = dr.getName();
                TrainProperties tp = TrainProperties.get(trainname);
                if (tp != null && tp.getHolder().isValid()) {
                    depreclist.add(dr);
                } else if (trainname != null) {
                    trainlist.dataconfig.set(trainname, null);
                    stapidslist.dataconfig.set(pidspath, null);
                    trainlist.save();
                    stapidslist.save();
                }
            }
        }
        return depreclist;
    }

    public int getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

}
