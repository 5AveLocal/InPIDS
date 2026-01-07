package me.fiveave.inpids;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static me.fiveave.inpids.main.*;

/// Station time list class
class statimelist {
    /// File object
    private final File file;
    /// List of station codes
    private final ArrayList<String> stacode;
    /// List of station names
    private final ArrayList<String[]> staname;
    /// List of platforms
    private final ArrayList<String> plat;
    /// List of arrival times
    private final ArrayList<Integer> time;
    /// List of booleans on whether train will stop at station
    private final ArrayList<Boolean> stop;
    /// Number of stations
    private int size;

    /// Station time list object
    ///
    /// @param linesys Line system (line, train type, destination, etc.)
    // Initialize constructor
    statimelist(String linesys) {
        file = new File(plugin.getDataFolder() + "/statimelist/" + linesys + ".csv");
        stacode = new ArrayList<>();
        staname = new ArrayList<>();
        plat = new ArrayList<>();
        time = new ArrayList<>();
        stop = new ArrayList<>();
        readFile();
    }

    /// Gets time needed to go to target station
    ///
    /// @param trainname Train name
    /// @param targetsta Target station
    /// @return Time to target station
    static int getTimeToStation(String trainname, String targetsta) {
        String linesys = trainlist.dataconfig.getString(trainname + ".linesys");
        String location = trainlist.dataconfig.getString(trainname + ".location");
        int time = trainlist.dataconfig.getInt(trainname + ".time");
        // Integer.MIN_VALUE meaning train is null, or has already passed station
        if (linesys == null) {
            return Integer.MIN_VALUE;
        }
        statimelist stl = stlmap.get(linesys);
        int currentstaindex = stl.getStaIndex(location);
        int targetstaindex = stl.getStaIndex(targetsta);
        int totaltime = 0;
        // Early return for target is previous of current
        if (targetstaindex < currentstaindex) {
            return Integer.MIN_VALUE;
        }
        // Is station in loop this station?
        for (int i = currentstaindex; i <= targetstaindex; i++) {
            totaltime += i == currentstaindex ? time : stl.getTime().get(i);
        }
        return totaltime;
    }

    /// @return List of times to station on linesys
    ArrayList<Integer> getTime() {
        return time;
    }

    /// @return List of platforms on linesys
    ArrayList<String> getPlat() {
        return plat;
    }

    /// @return List of station names on linesys
    ArrayList<String[]> getStaname() {
        return staname;
    }

    /// @return List of station codes on linesys
    ArrayList<String> getStacode() {
        return stacode;
    }

    /// Reads .csv file in statimelist folder
    // Read .csv
    private void readFile() {
        try {
            int length = 0;
            // Temporary ArrayLists
            ArrayList<String> stacodelist = new ArrayList<>();
            ArrayList<String[]> stanamelist = new ArrayList<>();
            ArrayList<String> platlist = new ArrayList<>();
            ArrayList<Integer> timelist = new ArrayList<>();
            ArrayList<Boolean> stoplist = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            // Read all lines in file
            while ((line = br.readLine()) != null) {
                int namesize = line.split(",").length - 4;
                String[] linesplit = line.split(",");
                String thiscode = linesplit[0];
                String[] thisstaname = new String[namesize];
                System.arraycopy(linesplit, 1, thisstaname, 0, namesize);
                String thisplat = linesplit[namesize + 1];
                int thistime = Integer.parseInt(linesplit[namesize + 2]);
                boolean thisstop = linesplit[namesize + 3].equals("stop");
                // Add to list
                stacodelist.add(thiscode);
                stanamelist.add(thisstaname);
                platlist.add(thisplat);
                timelist.add(thistime);
                stoplist.add(thisstop);
                // Increase length
                length++;
            }
            // Add all items into list
            stacode.addAll(stacodelist);
            staname.addAll(stanamelist);
            plat.addAll(platlist);
            time.addAll(timelist);
            stop.addAll(stoplist);
            // Set length
            size = length;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /// Gets the station index for a station name
    ///
    /// @param sta Station name
    /// @return Station index
    int getStaIndex(String sta) {
        for (int i = 0; i < size; i++) {
            if (stacode.get(i).equals(sta)) {
                return i;
            }
        }
        // Cannot find
        return -1;
    }

    /// @return List of booleans on whether train will stop at station on linesys
    ArrayList<Boolean> getStop() {
        return stop;
    }

    /// @return Number of stations on linesys
    int getSize() {
        return size;
    }
}