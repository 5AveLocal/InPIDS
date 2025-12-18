package me.fiveave.inpids;

import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

import static me.fiveave.inpids.main.stapidslist;
import static me.fiveave.inpids.main.trainlist;
import static me.fiveave.inpids.statimelist.getTimeToStation;

public class deprec {
    // Departure records in a PIDS display
    private String name; // Name of data (e.g. train name, info name)
    //    private String type; // Type of data (e.g. train, info)
    private int time; // -1 for N/A

    deprec(String depno) {
        name = stapidslist.dataconfig.getString(depno + ".name");
//        type = stapidslist.dataconfig.getString(depno + ".type");
        time = stapidslist.dataconfig.getInt(depno + ".time");
    }

    // Modify and sort PIDS list data for a platform
    static void updatePlatPidsList(String stacode, int plat, String trainname) {
        String staplat = stacode + "." + plat;
        // Get PIDS list, convert to record
        int timetosta = getTimeToStation(trainname, stacode);
        // If train does not exist then delete record
        TrainProperties tp = TrainProperties.get(trainname);
        if ((tp == null || !tp.getHolder().isValid()) && trainname != null) {
            trainlist.dataconfig.set(trainname, null);
            timetosta = Integer.MIN_VALUE;
        }
        // Add trains into list if valid, and find if list contains this train
        ArrayList<deprec> depreclist = getDeprecList(trainname, staplat, timetosta);
        // Sort records by arrival times
        ArrayList<deprec> newdepreclist = getNewDeprecList(depreclist);
        // Update PIDS list
        updatePidsList(newdepreclist, staplat);
    }

    static ArrayList<deprec> getNewDeprecList(ArrayList<deprec> depreclist) {
        ArrayList<deprec> newdepreclist = new ArrayList<>();
        while (!depreclist.isEmpty()) {
            int minindex = getMinPidsRecIndex(depreclist);
            newdepreclist.add(depreclist.get(minindex));
            depreclist.remove(minindex);
        }
        return newdepreclist;
    }

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

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
            String pidspath = staplat + ".departures." + dep;
            stapidslist.dataconfig.set(pidspath + ".name", newdepreclist.get(dep).getName());
//                    stapidslist.dataconfig.set(pidspath + ".type", newdepreclist.get(dep).getType());
            stapidslist.dataconfig.set(pidspath + ".time", newdepreclist.get(dep).getTime());
        }
    }

    static ArrayList<deprec> getDeprecList(String trainname, String staplat, int timetosta) {
        ArrayList<deprec> depreclist = new ArrayList<>();
        int foundtrainindex = -1;
        String deppath = staplat + ".departures";
        ConfigurationSection cs = stapidslist.dataconfig.getConfigurationSection(deppath);
        if (cs != null) {
            for (String dep : cs.getKeys(false)) {
                String pidspath = staplat + ".departures." + dep;
                // If time to station is -1, do not add into list
                if (timetosta == Integer.MIN_VALUE) {
                    stapidslist.dataconfig.set(pidspath, null);
                    continue;
                }
                deprec dr = new deprec(pidspath);
                String deptrainname = dr.getName();
                if (deptrainname.equals(trainname)) {
                    foundtrainindex = Integer.parseInt(dep);
                }
                depreclist.add(dr);
            }
        }
        if (timetosta != Integer.MIN_VALUE) {
            // Modify or add this train
            if (foundtrainindex != -1) {
                depreclist.get(foundtrainindex).setTime(timetosta);
            } else {
                deprec dr = new deprec(staplat + ".departures." + depreclist.size());
                dr.setName(trainname);
                dr.setTime(timetosta);
                depreclist.add(dr);
            }
        }
        return depreclist;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
